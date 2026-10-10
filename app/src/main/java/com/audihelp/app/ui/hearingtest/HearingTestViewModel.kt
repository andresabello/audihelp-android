package com.audihelp.app.ui.hearingtest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audihelp.app.audio.AudioEngineController
import com.audihelp.app.data.repository.HearingTestRepository
import com.audihelp.app.data.repository.ProfileRepository
import com.audihelp.app.domain.enums.EarSide
import com.audihelp.app.domain.enums.PatternType
import com.audihelp.app.domain.enums.TrialResponse
import com.audihelp.app.domain.model.HearingAcuityScore
import com.audihelp.app.domain.model.HearingTestSession
import com.audihelp.app.domain.model.HearingThreshold
import com.audihelp.app.domain.model.calculateHearingAcuity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HearingTestUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val sessions: List<HearingTestSession> = emptyList(),
    val activeSession: HearingTestSession? = null,
    val currentFrequencyIndex: Int = 0,
    val currentLevelDbfs: Float = -40f,
    val selectedEar: EarSide = EarSide.RIGHT,
    val selectedPattern: PatternType = PatternType.TONE,
    val isTonePlaying: Boolean = false,
    val isTestComplete: Boolean = false,
    // Populated after test completes
    val acuityScore: HearingAcuityScore? = null,
    // Accumulates responses during the active test to compute score locally
    val recordedThresholds: List<HearingThreshold> = emptyList(),
    // Profile name dialog shown after test completes
    val showProfileNameDialog: Boolean = false,
    val pendingSessionId: String? = null,
    val profileName: String = "",
    // Set to true after profile is created — triggers navigation to Profile tab
    val navigateToProfile: Boolean = false,
)

val TEST_FREQUENCIES = listOf(250, 500, 1000, 2000, 4000, 8000)

@HiltViewModel
class HearingTestViewModel @Inject constructor(
    private val repository: HearingTestRepository,
    private val profileRepository: ProfileRepository,
    private val engine: AudioEngineController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HearingTestUiState())
    val uiState: StateFlow<HearingTestUiState> = _uiState

    init { loadSessions() }

    fun loadSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getSessions()
                .onSuccess { sessions -> _uiState.update { it.copy(sessions = sessions, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
        }
    }

    fun selectEar(ear: EarSide) = _uiState.update { it.copy(selectedEar = ear) }
    fun selectPattern(pattern: PatternType) = _uiState.update { it.copy(selectedPattern = pattern) }
    fun setLevel(db: Float) {
        _uiState.update { it.copy(currentLevelDbfs = db) }
        if (_uiState.value.isTonePlaying) playTone()
    }

    fun startTest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val ear = _uiState.value.selectedEar
            repository.startSession(earTested = ear, notes = null)
                .onSuccess { session ->
                    _uiState.update {
                        it.copy(
                            activeSession = session,
                            isLoading = false,
                            currentFrequencyIndex = 0,
                            isTestComplete = false,
                            acuityScore = null,
                            recordedThresholds = emptyList(),
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
        }
    }

    fun playTone() {
        val state = _uiState.value
        val freq = TEST_FREQUENCIES[state.currentFrequencyIndex].toFloat()
        engine.startTone(freq, state.currentLevelDbfs, state.selectedPattern.ordinal)
        _uiState.update { it.copy(isTonePlaying = true) }
    }

    fun stopTone() {
        engine.stopTone()
        _uiState.update { it.copy(isTonePlaying = false) }
    }

    fun recordResponse(response: TrialResponse) {
        val state = _uiState.value
        val session = state.activeSession ?: return
        stopTone()
        viewModelScope.launch {
            repository.recordTrial(
                sessionId = session.id,
                ear = state.selectedEar,
                frequencyHz = TEST_FREQUENCIES[state.currentFrequencyIndex],
                levelDbfs = state.currentLevelDbfs,
                patternType = state.selectedPattern,
                response = response,
            ).onSuccess { trial ->
                // Accumulate threshold locally so we can score immediately on completion
                val threshold = HearingThreshold(
                    id = trial.id,
                    ear = trial.ear,
                    frequencyHz = trial.frequencyHz,
                    levelDbfs = trial.levelDbfs,
                    response = trial.response,
                    createdAt = trial.createdAt,
                )
                val updated = state.recordedThresholds + threshold
                val nextIndex = state.currentFrequencyIndex + 1
                if (nextIndex >= TEST_FREQUENCIES.size) {
                    _uiState.update { it.copy(recordedThresholds = updated) }
                    completeTest(session.id)
                } else {
                    _uiState.update {
                        it.copy(
                            currentFrequencyIndex = nextIndex,
                            currentLevelDbfs = -40f,
                            recordedThresholds = updated,
                        )
                    }
                }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    private fun completeTest(sessionId: String) {
        viewModelScope.launch {
            repository.completeSession(sessionId)
                .onSuccess { session ->
                    val score = calculateHearingAcuity(_uiState.value.recordedThresholds)
                    val ear = _uiState.value.selectedEar.label
                    _uiState.update {
                        it.copy(
                            activeSession = session,
                            isTestComplete = true,
                            acuityScore = score,
                            showProfileNameDialog = true,
                            pendingSessionId = sessionId,
                            profileName = "Test – $ear",
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
            loadSessions()
        }
    }

    fun setProfileName(name: String) = _uiState.update { it.copy(profileName = name) }

    fun saveProfile() {
        val state = _uiState.value
        if (state.profileName.isBlank() || state.pendingSessionId == null) return
        viewModelScope.launch {
            profileRepository.createProfile(
                name = state.profileName,
                notes = null,
                sourceSessionId = state.pendingSessionId,
            ).onSuccess { profile ->
                profileRepository.activateProfile(profile.id)
                _uiState.update { it.copy(showProfileNameDialog = false, navigateToProfile = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onNavigatedToProfile() = _uiState.update { it.copy(navigateToProfile = false) }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    override fun onCleared() {
        super.onCleared()
        engine.stopTone()
    }
}
