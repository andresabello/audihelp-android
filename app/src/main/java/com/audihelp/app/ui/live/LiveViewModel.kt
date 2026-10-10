package com.audihelp.app.ui.live

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audihelp.app.audio.AudioEngineController
import com.audihelp.app.audio.UsbAudioDeviceState
import com.audihelp.app.audio.UsbDeviceManager
import com.audihelp.app.data.repository.DspPresetRepository
import com.audihelp.app.data.repository.LiveSessionRepository
import com.audihelp.app.data.repository.ProfileRepository
import com.audihelp.app.domain.model.DspPreset
import com.audihelp.app.domain.model.HearingProfile
import com.audihelp.app.domain.model.LiveSession
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OutputRoute { INTERNAL_SPEAKER, WIRED_HEADSET, USB_AUDIO, BLUETOOTH, UNKNOWN }

data class LiveUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val usbMic: UsbAudioDeviceState = UsbAudioDeviceState(),
    val isLiveRunning: Boolean = false,
    val activeSession: LiveSession? = null,
    val profiles: List<HearingProfile> = emptyList(),
    val selectedProfileId: String? = null,
    val presets: List<DspPreset> = emptyList(),
    val selectedPresetId: String? = null,
    val inputLevelDbfs: Float = -80f,
    val outputLevelDbfs: Float = -80f,
    val outputRoute: OutputRoute = OutputRoute.UNKNOWN,
    val sessions: List<LiveSession> = emptyList(),
)

@HiltViewModel
class LiveViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usbManager: UsbDeviceManager,
    private val engine: AudioEngineController,
    private val liveRepo: LiveSessionRepository,
    private val presetRepo: DspPresetRepository,
    private val profileRepo: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveUiState())
    val uiState: StateFlow<LiveUiState> = _uiState

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var meterJob: Job? = null

    init {
        viewModelScope.launch {
            usbManager.deviceState.collect { state ->
                _uiState.update { it.copy(usbMic = state) }
            }
        }
        loadProfiles()
        loadPresets()
        loadSessions()
    }

    fun onResume() {
        usbManager.register()
        refreshOutputRoute()
    }

    fun onPause() = usbManager.unregister()

    fun refreshOutputRoute() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val route = when {
            devices.any { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                          it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                          it.type == AudioDeviceInfo.TYPE_LINE_ANALOG ||
                          it.type == AudioDeviceInfo.TYPE_LINE_DIGITAL } -> OutputRoute.WIRED_HEADSET
            devices.any { it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                          it.type == AudioDeviceInfo.TYPE_USB_ACCESSORY ||
                          it.type == AudioDeviceInfo.TYPE_USB_HEADSET } -> OutputRoute.USB_AUDIO
            devices.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                          it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO } -> OutputRoute.BLUETOOTH
            devices.any { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER } -> OutputRoute.INTERNAL_SPEAKER
            else -> OutputRoute.UNKNOWN
        }
        _uiState.update { it.copy(outputRoute = route) }
    }

    fun selectProfile(profileId: String) {
        _uiState.update { it.copy(selectedProfileId = profileId) }
        // Auto-select a matching DSP preset ("Audiogram: <profile name>")
        val profileName = _uiState.value.profiles.firstOrNull { it.id == profileId }?.name ?: return
        val matchingPreset = _uiState.value.presets.firstOrNull {
            it.name.equals("Audiogram: $profileName", ignoreCase = true)
        }
        if (matchingPreset != null) {
            _uiState.update { it.copy(selectedPresetId = matchingPreset.id) }
        }
    }

    fun selectPreset(presetId: String) = _uiState.update { it.copy(selectedPresetId = presetId) }

    fun startLiveSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Apply selected DSP preset before starting audio
            val preset = _uiState.value.presets.firstOrNull { it.id == _uiState.value.selectedPresetId }
            preset?.config?.let { engine.applyDspPreset(it) }

            val started = engine.startLiveMode()
            if (!started) {
                _uiState.update { it.copy(error = "Could not open audio — check mic permissions", isLoading = false) }
                return@launch
            }

            liveRepo.startSession(presetId = _uiState.value.selectedPresetId, profileId = null)
                .onSuccess { session ->
                    _uiState.update { it.copy(activeSession = session, isLiveRunning = true, isLoading = false) }
                    startMeterPolling()
                }
                .onFailure { e ->
                    // Session logging failed but audio is running — keep going
                    _uiState.update { it.copy(isLiveRunning = true, isLoading = false, error = e.message) }
                    startMeterPolling()
                }
        }
    }

    fun stopLiveSession() {
        meterJob?.cancel()
        meterJob = null
        engine.stopLiveMode()
        val session = _uiState.value.activeSession
        viewModelScope.launch {
            if (session != null) {
                liveRepo.endSession(session.id)
                    .onSuccess { loadSessions() }
            }
            _uiState.update { it.copy(isLiveRunning = false, activeSession = null) }
        }
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            profileRepo.getProfiles().onSuccess { profiles ->
                val activeId = profiles.firstOrNull { it.isActive }?.id
                _uiState.update { it.copy(profiles = profiles, selectedProfileId = activeId) }
            }
        }
    }

    private fun loadPresets() {
        viewModelScope.launch {
            presetRepo.getPresets().onSuccess { presets ->
                val activeId = presets.firstOrNull { it.isActive }?.id
                _uiState.update { it.copy(presets = presets, selectedPresetId = activeId) }
            }
        }
    }

    private fun loadSessions() {
        viewModelScope.launch {
            liveRepo.getSessions().onSuccess { sessions -> _uiState.update { it.copy(sessions = sessions) } }
        }
    }

    private fun startMeterPolling() {
        meterJob?.cancel()
        meterJob = viewModelScope.launch {
            while (isActive) {
                _uiState.update {
                    it.copy(
                        inputLevelDbfs  = engine.getInputLevelDbfs(),
                        outputLevelDbfs = engine.getOutputLevelDbfs(),
                    )
                }
                delay(80)
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    override fun onCleared() {
        super.onCleared()
        meterJob?.cancel()
        engine.stopLiveMode()
    }
}
