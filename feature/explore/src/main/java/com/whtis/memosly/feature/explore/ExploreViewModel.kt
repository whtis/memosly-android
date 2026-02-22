package com.whtis.memosly.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.whtis.memosly.core.common.AnalyticsHelper
import com.whtis.memosly.core.data.repository.AuthRepository
import com.whtis.memosly.core.data.repository.MemoRepository
import com.whtis.memosly.core.data.repository.UserRepository
import com.whtis.memosly.core.network.ServerVersion
import com.whtis.memosly.core.network.TokenManager
import com.whtis.memosly.core.model.Memo
import com.whtis.memosly.core.model.MemoRelationType
import com.whtis.memosly.core.model.Reaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val memoRepository: MemoRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val tokenManager: TokenManager,
    private val analyticsHelper: AnalyticsHelper,
) : ViewModel() {

    val serverUrl: String get() = authRepository.serverUrl ?: ""

    val authHeaders: Map<String, String>
        get() = tokenManager.accessToken.value?.let { token ->
            when (tokenManager.serverVersion.value) {
                ServerVersion.V025 -> mapOf("Cookie" to "user_session=$token")
                else -> mapOf("Authorization" to "Bearer $token")
            }
        } ?: emptyMap()

    private val _reactionOverrides = MutableStateFlow<Map<String, List<Reaction>>>(emptyMap())
    val reactionOverrides: StateFlow<Map<String, List<Reaction>>> = _reactionOverrides.asStateFlow()

    private val _emojiPickerMemoId = MutableStateFlow<String?>(null)
    val emojiPickerMemoId: StateFlow<String?> = _emojiPickerMemoId.asStateFlow()

    private val _commentSheetMemoId = MutableStateFlow<String?>(null)
    val commentSheetMemoId: StateFlow<String?> = _commentSheetMemoId.asStateFlow()

    private val _commentPreviews = MutableStateFlow<Map<String, List<Memo>>>(emptyMap())
    val commentPreviews: StateFlow<Map<String, List<Memo>>> = _commentPreviews.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _creatorNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val creatorNames: StateFlow<Map<String, String>> = _creatorNames.asStateFlow()

    // Increment to force paging refresh
    private val _refreshTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val memos: Flow<PagingData<Memo>> = combine(
        _searchQuery.debounce(300),
        _refreshTrigger,
    ) { query, _ -> query }
        .flatMapLatest { query ->
            val parts = mutableListOf("visibility in [\"PUBLIC\", \"PROTECTED\"]")
            if (query.isNotBlank()) {
                parts.add("content.contains(\"$query\")")
            }
            memoRepository.getMemosPaged(parts.joinToString(" && "))
        }
        .cachedIn(viewModelScope)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            analyticsHelper.logEvent("explore_search", mapOf("query_length" to query.length.toString()))
        }
    }

    fun resolveCreators(creators: List<String>) {
        val unknown = creators.filter { it.isNotBlank() && it !in _creatorNames.value }
        if (unknown.isEmpty()) return
        viewModelScope.launch {
            for (creator in unknown.distinct()) {
                val id = creator.substringAfterLast("/").toIntOrNull() ?: continue
                try {
                    val user = userRepository.getUser(id)
                    _creatorNames.value = _creatorNames.value + (creator to user.nickname.ifBlank { user.username })
                } catch (_: Exception) {
                    // Silently skip — will show fallback ID
                }
            }
        }
    }

    private val _scrollToTop = MutableStateFlow(false)
    val scrollToTop: StateFlow<Boolean> = _scrollToTop.asStateFlow()

    fun refresh() {
        _reactionOverrides.value = emptyMap()
        _commentPreviews.value = emptyMap()
        _creatorNames.value = emptyMap()
        _refreshTrigger.value++
    }

    fun refreshAndScrollToTop() {
        refresh()
        _scrollToTop.value = true
    }

    fun consumeScrollToTop() {
        _scrollToTop.value = false
    }

    fun loadCommentPreviews(memos: List<Memo>) {
        val memosWithComments = memos.filter { memo ->
            memo.relations.any { it.type == MemoRelationType.COMMENT } &&
                memo.uid !in _commentPreviews.value
        }
        if (memosWithComments.isEmpty()) return
        viewModelScope.launch {
            for (memo in memosWithComments) {
                try {
                    val comments = memoRepository.getComments(memo.uid)
                    _commentPreviews.value = _commentPreviews.value +
                        (memo.uid to comments.take(3))
                    resolveCreators(comments.take(3).map { it.creator })
                } catch (_: Exception) {}
            }
        }
    }

    fun toggleReaction(memoUid: String, reactionType: String) {
        _emojiPickerMemoId.value = null
        viewModelScope.launch {
            try {
                val currentUserName = authRepository.currentUser.value?.name
                val currentReactions = _reactionOverrides.value[memoUid]
                    ?: memoRepository.getReactions(memoUid)
                val existing = currentReactions.find {
                    it.reactionType == reactionType && it.creator == currentUserName
                }
                if (existing != null) {
                    memoRepository.deleteReaction(existing.id, memoUid)
                } else {
                    memoRepository.upsertReaction(memoUid, reactionType)
                }
                val reactions = memoRepository.getReactions(memoUid)
                _reactionOverrides.value = _reactionOverrides.value + (memoUid to reactions)
            } catch (_: Exception) {}
        }
    }

    fun toggleEmojiPicker(memoUid: String) {
        _emojiPickerMemoId.value = if (_emojiPickerMemoId.value == memoUid) null else memoUid
    }

    fun openCommentSheet(memoUid: String) {
        _commentSheetMemoId.value = memoUid
    }

    fun closeCommentSheet() {
        _commentSheetMemoId.value = null
    }

    fun archiveMemo(memoUid: String) {
        viewModelScope.launch {
            try {
                memoRepository.archiveMemo(memoUid)
                refresh()
            } catch (_: Exception) {}
        }
    }

    fun deleteMemo(memoUid: String) {
        viewModelScope.launch {
            try {
                memoRepository.deleteMemo(memoUid)
                refresh()
            } catch (_: Exception) {}
        }
    }

    fun sendComment(memoUid: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            try {
                val newComment = memoRepository.createComment(memoUid, content)
                _commentSheetMemoId.value = null
                // Immediately show the new comment in previews
                val existing = _commentPreviews.value[memoUid] ?: emptyList()
                _commentPreviews.value = _commentPreviews.value +
                    (memoUid to (existing + newComment).takeLast(3))
                // Then reload full list in background
                try {
                    val comments = memoRepository.getComments(memoUid)
                    _commentPreviews.value = _commentPreviews.value +
                        (memoUid to comments.take(3))
                } catch (_: Exception) {}
            } catch (_: Exception) {}
        }
    }
}
