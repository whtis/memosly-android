package com.whtis.memosly.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.whtis.memosly.core.data.repository.AuthRepository
import com.whtis.memosly.core.data.repository.MemoRepository
import com.whtis.memosly.core.data.repository.UserRepository
import com.whtis.memosly.core.network.ServerVersion
import com.whtis.memosly.core.network.TokenManager
import com.whtis.memosly.core.model.Memo
import com.whtis.memosly.core.model.MemoRelationType
import com.whtis.memosly.core.model.Reaction
import com.whtis.memosly.core.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val memoRepository: MemoRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val tokenManager: TokenManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    private val _creatorNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val creatorNames: StateFlow<Map<String, String>> = _creatorNames.asStateFlow()

    private val _filter = MutableStateFlow<String?>(null)
    val filter: StateFlow<String?> = _filter.asStateFlow()

    private val _activeTag = MutableStateFlow<String?>(null)
    val activeTag: StateFlow<String?> = _activeTag.asStateFlow()

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Increment to force paging refresh
    private val _refreshTrigger = MutableStateFlow(0)

    init {
        savedStateHandle.get<String>("tag")?.let { tag -> setTagFilter(tag) }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val memos: Flow<PagingData<Memo>> = combine(
        _filter,
        _showArchived,
        _searchQuery.debounce(300),
        _refreshTrigger,
    ) { filter, archived, query, _ ->
        Triple(filter, archived, query)
    }.flatMapLatest { (filter, archived, query) ->
        val userId = authRepository.currentUser.value?.id
        val combinedFilter = buildFilter(filter, query, userId)
        memoRepository.getMemosPaged(
            filter = combinedFilter,
            state = if (archived) "ARCHIVED" else null,
        )
    }.cachedIn(viewModelScope)

    fun setFilter(filter: String?) {
        _filter.value = filter
    }

    fun setTagFilter(tag: String) {
        _activeTag.value = tag
        updateFilter()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleArchived() {
        _showArchived.value = !_showArchived.value
        updateFilter()
    }

    fun setArchived(archived: Boolean) {
        if (_showArchived.value != archived) {
            _showArchived.value = archived
            updateFilter()
        }
    }

    fun clearFilter() {
        _activeTag.value = null
        _showArchived.value = false
        _filter.value = null
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

    private fun resolveCreators(creators: List<String>) {
        val unknown = creators.filter { it.isNotBlank() && it !in _creatorNames.value }
        if (unknown.isEmpty()) return
        viewModelScope.launch {
            for (creator in unknown.distinct()) {
                val id = creator.substringAfterLast("/").toIntOrNull() ?: continue
                try {
                    val user = userRepository.getUser(id)
                    _creatorNames.value = _creatorNames.value +
                        (creator to user.nickname.ifBlank { user.username })
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
                    resolveCreators(comments.take(3).map { it.creator })
                } catch (_: Exception) {}
            } catch (_: Exception) {}
        }
    }

    fun archiveMemo(memoUid: String) {
        viewModelScope.launch {
            try {
                if (_showArchived.value) {
                    memoRepository.restoreMemo(memoUid)
                } else {
                    memoRepository.archiveMemo(memoUid)
                }
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

    private fun updateFilter() {
        val parts = mutableListOf<String>()
        _activeTag.value?.let { parts.add("tag in [\"$it\"]") }
        _filter.value = if (parts.isEmpty()) null else parts.joinToString(" && ")
    }

    private fun buildFilter(tagFilter: String?, searchQuery: String, userId: Int?): String? {
        val parts = mutableListOf<String>()
        userId?.let { parts.add("creator_id == $it") }
        tagFilter?.let { parts.add(it) }
        if (searchQuery.isNotBlank()) {
            parts.add("content.contains(\"$searchQuery\")")
        }
        return if (parts.isEmpty()) null else parts.joinToString(" && ")
    }
}
