package com.audihelp.app.ui.dsplab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audihelp.app.audio.AudioEngineController
import com.audihelp.app.data.repository.DspPresetRepository
import com.audihelp.app.data.repository.ProfileRepository
import com.audihelp.app.domain.model.CompressorConfig
import com.audihelp.app.domain.model.DspConfig
import com.audihelp.app.domain.model.DspPreset
import com.audihelp.app.domain.model.EqBand
import com.audihelp.app.domain.model.FreqTransformConfig
import com.audihelp.app.domain.model.HearingProfile
import com.audihelp.app.domain.model.LimiterConfig
import com.audihelp.app.domain.model.ModulationConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DspLabUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    // Profiles
    val profiles: List<HearingProfile> = emptyList(),
    val selectedProfileId: String? = null,
    // Presets for the selected profile
    val presets: List<DspPreset> = emptyList(),
    val selectedPreset: DspPreset? = null,
    // Working config (may differ from selectedPreset.config when dirty)
    val workingConfig: DspConfig = DspConfig.default(),
    val isDirty: Boolean = false,
    // New preset dialog
    val showNewPresetDialog: Boolean = false,
    val newPresetName: String = "",
)

@HiltViewModel
class DspLabViewModel @Inject constructor(
    private val repository: DspPresetRepository,
    private val profileRepository: ProfileRepository,
    private val engine: AudioEngineController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DspLabUiState())
    val uiState: StateFlow<DspLabUiState> = _uiState

    init { loadProfiles() }

    // ── Profiles ──────────────────────────────────────────────────────────────

    private fun loadProfiles() {
        viewModelScope.launch {
            profileRepository.getProfiles().onSuccess { profiles ->
                val activeId = profiles.firstOrNull { it.isActive }?.id
                _uiState.update { it.copy(profiles = profiles, selectedProfileId = activeId) }
                if (activeId != null) loadPresetsForProfile(activeId)
            }
        }
    }

    fun selectProfile(profileId: String) {
        _uiState.update { it.copy(selectedProfileId = profileId, selectedPreset = null, workingConfig = DspConfig.default(), isDirty = false) }
        loadPresetsForProfile(profileId)
    }

    // ── Presets ───────────────────────────────────────────────────────────────

    private fun loadPresetsForProfile(profileId: String) {
        viewModelScope.launch {
            repository.getPresetsByProfile(profileId).onSuccess { presets ->
                val active = presets.firstOrNull { it.isActive } ?: presets.firstOrNull()
                _uiState.update {
                    it.copy(
                        presets = presets,
                        selectedPreset = active,
                        workingConfig = active?.config ?: DspConfig.default(),
                        isDirty = false,
                    )
                }
                active?.let { engine.applyDspPreset(it.config) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun selectPreset(preset: DspPreset) {
        _uiState.update { it.copy(selectedPreset = preset, workingConfig = preset.config, isDirty = false) }
        engine.applyDspPreset(preset.config)
    }

    // ── Save / Update ─────────────────────────────────────────────────────────

    /** Overwrite the currently selected preset with the working config. */
    fun updateCurrentPreset() {
        val state = _uiState.value
        val preset = state.selectedPreset ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.updatePreset(preset.id, preset.name, preset.description, state.workingConfig)
                .onSuccess { updated ->
                    _uiState.update { it.copy(selectedPreset = updated, isDirty = false, isLoading = false) }
                    state.selectedProfileId?.let { loadPresetsForProfile(it) }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
        }
    }

    /** Show the dialog to name and save the config as a brand-new preset. */
    fun showNewPresetDialog() = _uiState.update { it.copy(showNewPresetDialog = true, newPresetName = "") }
    fun dismissNewPresetDialog() = _uiState.update { it.copy(showNewPresetDialog = false, newPresetName = "") }
    fun setNewPresetName(name: String) = _uiState.update { it.copy(newPresetName = name) }

    fun saveAsNewPreset() {
        val state = _uiState.value
        if (state.newPresetName.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.createPreset(
                name = state.newPresetName,
                description = null,
                config = state.workingConfig,
                profileId = state.selectedProfileId,
            ).onSuccess { created ->
                _uiState.update { it.copy(selectedPreset = created, isDirty = false, isLoading = false, showNewPresetDialog = false, newPresetName = "") }
                state.selectedProfileId?.let { loadPresetsForProfile(it) }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
        }
    }

    fun deleteSelectedPreset() {
        val id = _uiState.value.selectedPreset?.id ?: return
        viewModelScope.launch {
            repository.deletePreset(id).onSuccess {
                _uiState.update { it.copy(selectedPreset = null, workingConfig = DspConfig.default(), isDirty = false) }
                _uiState.value.selectedProfileId?.let { loadPresetsForProfile(it) }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    // ── DSP config edits ──────────────────────────────────────────────────────

    fun updateEqBand(index: Int, band: EqBand) {
        val newEq = _uiState.value.workingConfig.eq.toMutableList().also { it[index] = band }
        updateConfig(_uiState.value.workingConfig.copy(eq = newEq))
        engine.setEqBand(index, band.freq, band.gainDb, band.q, band.type)
    }

    fun updateMasterGain(gainDb: Float) {
        updateConfig(_uiState.value.workingConfig.copy(gainDb = gainDb))
        engine.setMasterGain(gainDb)
    }

    fun updateCompressor(config: CompressorConfig) {
        updateConfig(_uiState.value.workingConfig.copy(compressor = config))
        engine.setCompressor(config.thresholdDbfs, config.ratio, config.attackMs, config.releaseMs, config.makeupGainDb)
    }

    fun updateLimiter(config: LimiterConfig) {
        updateConfig(_uiState.value.workingConfig.copy(limiter = config))
        engine.setLimiterCeiling(config.ceilingDbfs)
    }

    fun updateFreqTransform(config: FreqTransformConfig) {
        updateConfig(_uiState.value.workingConfig.copy(freqTransform = config))
        engine.setFreqShift(config.enabled, config.shiftHz)
    }

    fun updateModulation(config: ModulationConfig) {
        updateConfig(_uiState.value.workingConfig.copy(modulation = config))
        engine.setModulation(config.enabled, config.depth, config.rateHz)
    }

    private fun updateConfig(config: DspConfig) {
        _uiState.update { it.copy(workingConfig = config, isDirty = true) }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }
}
