package com.whtis.memosly.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whtis.memosly.core.data.repository.WorkspaceRepository
import com.whtis.memosly.core.model.IdentityProvider
import com.whtis.memosly.core.model.WorkspaceProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val profile: WorkspaceProfile? = null,
    val identityProviders: List<IdentityProvider> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val workspaceRepository: WorkspaceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        loadAdmin()
    }

    fun loadAdmin() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val profile = workspaceRepository.getWorkspaceProfile()
                val idps = workspaceRepository.listIdentityProviders()
                _uiState.value = AdminUiState(
                    profile = profile,
                    identityProviders = idps,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
