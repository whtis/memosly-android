package com.whtis.memosly.feature.memo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whtis.memosly.core.data.repository.AuthRepository
import com.whtis.memosly.core.data.repository.MemoRepository
import com.whtis.memosly.core.model.Memo
import com.whtis.memosly.core.model.Reaction
import com.whtis.memosly.core.network.ServerVersion
import com.whtis.memosly.core.network.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoDetailUiState(
    val memo: Memo? = null,
    val comments: List<Memo> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val commentText: String = "",
    val isSendingComment: Boolean = false,
    val showEmojiPicker: Boolean = false,
)

@HiltViewModel
class MemoDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memoRepository: MemoRepository,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val memoId: String = savedStateHandle["memoId"] ?: ""

    val serverUrl: String get() = tokenManager.serverUrl.value ?: ""

    val authHeaders: Map<String, String>
        get() = tokenManager.accessToken.value?.let { token ->
            when (tokenManager.serverVersion.value) {
                ServerVersion.V025 -> mapOf("Cookie" to "user_session=$token")
                else -> mapOf("Authorization" to "Bearer $token")
            }
        } ?: emptyMap()

    private val _uiState = MutableStateFlow(MemoDetailUiState())
    val uiState: StateFlow<MemoDetailUiState> = _uiState.asStateFlow()

    /** True if reactions or comments were modified, so parent list should refresh on back. */
    var hasChanges: Boolean = false
        private set

    init {
        loadMemo()
    }

    fun loadMemo() {
        if (memoId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val memo = memoRepository.getMemo(memoId)
                _uiState.value = _uiState.value.copy(
                    memo = memo,
                    isLoading = false,
                )
                val comments = try {
                    memoRepository.getComments(memoId)
                } catch (_: Exception) {
                    emptyList()
                }
                _uiState.value = _uiState.value.copy(comments = comments)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load memo",
                )
            }
        }
    }

    fun updateCommentText(text: String) {
        _uiState.value = _uiState.value.copy(commentText = text)
    }

    fun sendComment() {
        val text = _uiState.value.commentText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSendingComment = true)
            try {
                val newComment = memoRepository.createComment(memoId, text)
                // Clear input immediately after successful creation
                hasChanges = true
                _uiState.value = _uiState.value.copy(
                    commentText = "",
                    isSendingComment = false,
                    comments = _uiState.value.comments + newComment,
                )
                // Then reload full comment list in background
                try {
                    val comments = memoRepository.getComments(memoId)
                    _uiState.value = _uiState.value.copy(comments = comments)
                } catch (_: Exception) {}
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSendingComment = false)
            }
        }
    }

    fun toggleEmojiPicker() {
        _uiState.value = _uiState.value.copy(showEmojiPicker = !_uiState.value.showEmojiPicker)
    }

    fun toggleReaction(reactionType: String) {
        _uiState.value = _uiState.value.copy(showEmojiPicker = false)
        hasChanges = true
        viewModelScope.launch {
            try {
                val currentUserName = authRepository.currentUser.value?.name
                val existingReaction = _uiState.value.memo?.reactions?.find {
                    it.reactionType == reactionType && it.creator == currentUserName
                }
                if (existingReaction != null) {
                    memoRepository.deleteReaction(existingReaction.id, memoId)
                } else {
                    memoRepository.upsertReaction(memoId, reactionType)
                }
                val reactions = memoRepository.getReactions(memoId)
                _uiState.value = _uiState.value.copy(
                    memo = _uiState.value.memo?.copy(reactions = reactions),
                )
            } catch (_: Exception) {}
        }
    }

    fun archiveMemo(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                memoRepository.archiveMemo(memoId)
                onDone()
            } catch (_: Exception) {}
        }
    }

    fun restoreMemo() {
        viewModelScope.launch {
            try {
                val updated = memoRepository.restoreMemo(memoId)
                _uiState.value = _uiState.value.copy(memo = updated)
            } catch (_: Exception) {}
        }
    }

    fun deleteMemo(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                memoRepository.deleteMemo(memoId)
                onDeleted()
            } catch (_: Exception) {}
        }
    }
}
