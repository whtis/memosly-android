package com.whtis.memosly.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whtis.memosly.core.data.repository.AuthRepository
import com.whtis.memosly.core.data.repository.UserRepository
import com.whtis.memosly.core.model.User
import com.whtis.memosly.core.model.UserAccessToken
import com.whtis.memosly.core.model.Webhook
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val user: User? = null,
    val accessTokens: List<UserAccessToken> = emptyList(),
    val webhooks: List<Webhook> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val user = authRepository.currentUser.value
                if (user != null) {
                    val tokens = userRepository.listAccessTokens(user.id)
                    val webhooks = userRepository.listWebhooks(user.id)
                    _uiState.value = SettingsUiState(
                        user = user,
                        accessTokens = tokens,
                        webhooks = webhooks,
                        isLoading = false,
                    )
                } else {
                    _uiState.value = SettingsUiState(isLoading = false, error = "Not authenticated")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun createAccessToken(description: String) {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            try {
                userRepository.createAccessToken(user.id, description, null)
                val tokens = userRepository.listAccessTokens(user.id)
                _uiState.value = _uiState.value.copy(accessTokens = tokens)
            } catch (_: Exception) {}
        }
    }

    fun deleteAccessToken(token: String) {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            try {
                userRepository.deleteAccessToken(user.id, token)
                val tokens = userRepository.listAccessTokens(user.id)
                _uiState.value = _uiState.value.copy(accessTokens = tokens)
            } catch (_: Exception) {}
        }
    }

    fun createWebhook(name: String, url: String) {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            try {
                userRepository.createWebhook(name, url)
                val webhooks = userRepository.listWebhooks(user.id)
                _uiState.value = _uiState.value.copy(webhooks = webhooks)
            } catch (_: Exception) {}
        }
    }

    fun deleteWebhook(id: Int) {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            try {
                userRepository.deleteWebhook(id)
                val webhooks = userRepository.listWebhooks(user.id)
                _uiState.value = _uiState.value.copy(webhooks = webhooks)
            } catch (_: Exception) {}
        }
    }
}
