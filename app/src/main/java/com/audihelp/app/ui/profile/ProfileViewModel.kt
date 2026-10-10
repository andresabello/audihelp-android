package com.audihelp.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audihelp.app.data.repository.DspPresetRepository
import com.audihelp.app.data.repository.ProfileRepository
import com.audihelp.app.domain.model.HearingAcuityScore
import com.audihelp.app.domain.model.HearingProfile
import com.audihelp.app.domain.model.HearingThreshold
import com.audihelp.app.domain.model.calculateHearingAcuity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val profiles: List<HearingProfile> = emptyList(),
    val selectedProfile: HearingProfile? = null,
    val thresholds: List<HearingThreshold> = emptyList(),
    val acuityScore: HearingAcuityScore? = null,
    val showCreateDialog: Boolean = false,
    val newProfileName: String = "",
    val newProfileSessionId: String = "",
    // DSP generation
    val generatedPresetName: String? = null, // non-null = generation succeeded
    val dspDescription: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val dspRepository: DspPresetRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init { loadProfiles() }

    fun loadProfiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getProfiles()
                .onSuccess { profiles -> _uiState.update { it.copy(profiles = profiles, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
        }
    }

    fun selectProfile(profile: HearingProfile) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generatedPresetName = null, dspDescription = null, acuityScore = null) }
            repository.getProfile(profile.id)
                .onSuccess { full ->
                    val score = if (full.thresholds.isNotEmpty()) calculateHearingAcuity(full.thresholds) else null
                    _uiState.update { it.copy(selectedProfile = full, thresholds = full.thresholds, acuityScore = score, isLoading = false) }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
        }
    }

    fun activateProfile(profileId: String) {
        viewModelScope.launch {
            repository.activateProfile(profileId)
                .onSuccess { loadProfiles() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            repository.deleteProfile(profileId)
                .onSuccess {
                    _uiState.update { it.copy(selectedProfile = null, thresholds = emptyList(), generatedPresetName = null) }
                    loadProfiles()
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun setNewProfileName(name: String) = _uiState.update { it.copy(newProfileName = name) }
    fun setNewProfileSessionId(id: String) = _uiState.update { it.copy(newProfileSessionId = id) }
    fun showCreateDialog() = _uiState.update { it.copy(showCreateDialog = true) }
    fun dismissCreateDialog() = _uiState.update { it.copy(showCreateDialog = false, newProfileName = "", newProfileSessionId = "") }

    fun createProfile() {
        val state = _uiState.value
        if (state.newProfileName.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.createProfile(
                name = state.newProfileName,
                notes = null,
                sourceSessionId = state.newProfileSessionId.ifBlank { null },
            ).onSuccess {
                dismissCreateDialog()
                loadProfiles()
            }.onFailure { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
        }
    }

    /**
     * Converts the selected profile's hearing thresholds into a DspConfig
     * and saves it as a new DSP preset, ready to be loaded in Live mode.
     *
     * The generated config boosts EQ at frequencies where hearing loss is detected
     * and enables amplitude modulation for frequencies that are felt but not heard.
     */
    fun generateDspPreset() {
        val state = _uiState.value
        val profile = state.selectedProfile ?: return
        val thresholds = state.thresholds
        if (thresholds.isEmpty()) {
            _uiState.update { it.copy(error = "No threshold data. Run a hearing test first.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val config = thresholdsToDspConfig(thresholds)
            val description = describeGeneratedConfig(config, thresholds)
            val presetName = "Audiogram: ${profile.name}"
            dspRepository.createPreset(
                name = presetName,
                description = description,
                config = config,
                profileId = profile.id,
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        generatedPresetName = presetName,
                        dspDescription = description,
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun dismissGeneratedBanner() = _uiState.update { it.copy(generatedPresetName = null, dspDescription = null) }
    fun dismissError() = _uiState.update { it.copy(error = null) }
}
