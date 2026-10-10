package com.audihelp.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audihelp.app.BuildConfig
import com.audihelp.app.data.local.AppDataStore
import com.audihelp.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Error(val message: String) : AuthUiState
    object Success : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val dataStore: AppDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    private val _googleAuthUrl = MutableStateFlow<String?>(null)
    val googleAuthUrl: StateFlow<String?> = _googleAuthUrl

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    init {
        viewModelScope.launch {
            _isLoggedIn.value = !dataStore.getToken().first().isNullOrBlank()
        }
    }

    fun fetchGoogleAuthUrl() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.getGoogleRedirectUrl()
                .onSuccess { url -> _googleAuthUrl.value = url; _uiState.value = AuthUiState.Idle }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Failed to get auth URL") }
        }
    }

    fun handleOAuthCallback(token: String) {
        viewModelScope.launch {
            repository.saveTokenAndFetchUser(token)
                .onSuccess { _isLoggedIn.value = true; _uiState.value = AuthUiState.Success }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Login failed") }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _isLoggedIn.value = false
            _uiState.value = AuthUiState.Idle
        }
    }
}
