package com.whtis.memosly.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whtis.memosly.core.data.repository.InboxRepository
import com.whtis.memosly.core.model.InboxMessage
import com.whtis.memosly.core.model.InboxStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val messages: List<InboxMessage> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val inboxRepository: InboxRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadInbox()
    }

    fun loadInbox() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val messages = inboxRepository.listInbox()
                _uiState.value = NotificationsUiState(
                    messages = messages,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load notifications",
                )
            }
        }
    }

    fun markAsRead(message: InboxMessage) {
        if (message.status == InboxStatus.READ) return
        viewModelScope.launch {
            try {
                inboxRepository.updateInboxStatus(message.name, "READ")
                loadInbox()
            } catch (_: Exception) {}
        }
    }

    fun archive(message: InboxMessage) {
        viewModelScope.launch {
            try {
                inboxRepository.updateInboxStatus(message.name, "ARCHIVED")
                loadInbox()
            } catch (_: Exception) {}
        }
    }
}
