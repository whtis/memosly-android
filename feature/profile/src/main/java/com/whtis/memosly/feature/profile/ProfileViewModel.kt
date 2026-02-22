package com.whtis.memosly.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whtis.memosly.core.common.AnalyticsHelper
import com.whtis.memosly.core.data.repository.AuthRepository
import com.whtis.memosly.core.data.repository.UpdateRepository
import com.whtis.memosly.core.data.repository.UserRepository
import com.whtis.memosly.core.data.repository.WorkspaceRepository
import com.whtis.memosly.core.model.IdentityProvider
import com.whtis.memosly.core.model.User
import com.whtis.memosly.core.model.UserAccessToken
import com.whtis.memosly.core.model.UserRole
import com.whtis.memosly.core.model.UserStats
import com.whtis.memosly.core.model.Webhook
import com.whtis.memosly.core.model.WorkspaceProfile
import com.whtis.memosly.core.network.SessionPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data object UpToDate : UpdateCheckState
    data class UpdateAvailable(
        val latestVersion: String,
        val releaseNotes: String,
    ) : UpdateCheckState
    data class Error(val message: String) : UpdateCheckState
}

data class ProfileUiState(
    val user: User? = null,
    val stats: UserStats? = null,
    val accessTokens: List<UserAccessToken> = emptyList(),
    val webhooks: List<Webhook> = emptyList(),
    val workspaceProfile: WorkspaceProfile? = null,
    val identityProviders: List<IdentityProvider> = emptyList(),
    val isAdmin: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val appVersion: String = "",
    val updateCheckState: UpdateCheckState = UpdateCheckState.Idle,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val updateRepository: UpdateRepository,
    private val sessionPreferences: SessionPreferences,
    private val analyticsHelper: AnalyticsHelper,
    @Named("appVersion") private val appVersion: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val navModeFlow: StateFlow<String> = sessionPreferences.navModeFlow

    fun setNavMode(mode: String) {
        sessionPreferences.navMode = mode
        analyticsHelper.logEvent("nav_mode_switch", mapOf("mode" to mode))
    }

    fun logSupportAuthorClick() {
        analyticsHelper.logEvent("support_author_click")
    }

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val user = authRepository.currentUser.value
                if (user != null) {
                    val stats = userRepository.getUserStats(user.id)
                    val tokens = try { userRepository.listAccessTokens(user.id) } catch (_: Exception) { emptyList() }
                    val webhooks = try { userRepository.listWebhooks(user.id) } catch (_: Exception) { emptyList() }
                    val isAdmin = user.role == UserRole.HOST || user.role == UserRole.ADMIN

                    var wsProfile: WorkspaceProfile? = null
                    var idps: List<IdentityProvider> = emptyList()
                    if (isAdmin) {
                        try {
                            wsProfile = workspaceRepository.getWorkspaceProfile()
                            idps = workspaceRepository.listIdentityProviders()
                        } catch (_: Exception) {}
                    }

                    _uiState.value = ProfileUiState(
                        user = user,
                        stats = stats,
                        accessTokens = tokens,
                        webhooks = webhooks,
                        workspaceProfile = wsProfile,
                        identityProviders = idps,
                        isAdmin = isAdmin,
                        isLoading = false,
                        appVersion = appVersion,
                    )
                } else {
                    _uiState.value = ProfileUiState(isLoading = false, error = "Not authenticated")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
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

    fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(updateCheckState = UpdateCheckState.Checking)
            try {
                val info = updateRepository.checkForUpdate(appVersion)
                _uiState.value = _uiState.value.copy(
                    updateCheckState = if (info.hasUpdate) {
                        UpdateCheckState.UpdateAvailable(
                            latestVersion = info.latestVersion,
                            releaseNotes = info.releaseNotes,
                        )
                    } else {
                        UpdateCheckState.UpToDate
                    },
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    updateCheckState = UpdateCheckState.Error(e.message ?: "Unknown error"),
                )
            }
        }
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onComplete()
        }
    }
}
