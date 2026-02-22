package com.whtis.memosly.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whtis.memosly.core.common.AnalyticsHelper
import com.whtis.memosly.core.data.repository.AuthRepository
import com.whtis.memosly.core.network.ServerVersion
import com.whtis.memosly.core.network.SessionPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val serverVersion: ServerVersion = ServerVersion.V026,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRestoringSession: Boolean = true,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val analyticsHelper: AnalyticsHelper,
    private val sessionPreferences: SessionPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            serverUrl = sessionPreferences.serverUrl ?: "",
            serverVersion = ServerVersion.fromString(sessionPreferences.serverVersion),
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        restoreSession()
    }

    private var onLoginSuccessCallback: (() -> Unit)? = null

    fun setLoginSuccessCallback(callback: () -> Unit) {
        onLoginSuccessCallback = callback
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val restored = authRepository.restoreSession()
            _uiState.value = _uiState.value.copy(isRestoringSession = false)
            if (restored) {
                onLoginSuccessCallback?.invoke()
            }
        }
    }

    fun updateServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url, error = null)
    }

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username, error = null)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun updateServerVersion(version: ServerVersion) {
        _uiState.value = _uiState.value.copy(serverVersion = version, error = null)
    }

    fun signIn() {
        val state = _uiState.value
        if (state.serverUrl.isBlank() || state.username.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "All fields are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                authRepository.signIn(
                    serverUrl = state.serverUrl.trimEnd('/'),
                    username = state.username,
                    password = state.password,
                    version = state.serverVersion,
                )
                _uiState.value = _uiState.value.copy(isLoading = false)
                val host = try {
                    java.net.URI(state.serverUrl).host ?: state.serverUrl
                } catch (_: Exception) { state.serverUrl }
                analyticsHelper.logEvent("login_success", mapOf(
                    "server_host" to host,
                    "server_version" to state.serverVersion.name,
                ))
                onLoginSuccessCallback?.invoke()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Sign in failed",
                )
            }
        }
    }
}
