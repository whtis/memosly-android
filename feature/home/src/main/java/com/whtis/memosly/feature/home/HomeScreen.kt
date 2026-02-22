package com.whtis.memosly.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.whtis.memosly.core.ui.R as UiR
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.first
import com.whtis.memosly.core.model.Memo
import com.whtis.memosly.core.model.Reaction
import com.whtis.memosly.core.ui.component.LoadingContent
import com.whtis.memosly.core.markdown.MarkdownPreview
import com.whtis.memosly.core.ui.component.MemoCard
import com.whtis.memosly.core.ui.component.MediaViewerDialog
import com.whtis.memosly.core.ui.component.MemosDrawerContent
import com.whtis.memosly.core.ui.component.SidebarDestination
import com.whtis.memosly.core.ui.component.ViewableMedia
import com.whtis.memosly.core.ui.theme.MemosShapes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    onMemoClick: (String) -> Unit,
    onNewMemoClick: () -> Unit,
    onNavigate: (SidebarDestination) -> Unit,
    onProfileClick: () -> Unit,
    onEditMemo: (String) -> Unit = {},
    refreshOnReturn: Boolean = false,
    onRefreshConsumed: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val activeTag by viewModel.activeTag.collectAsStateWithLifecycle()
    val showArchived by viewModel.showArchived.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val reactionOverrides by viewModel.reactionOverrides.collectAsStateWithLifecycle()
    val emojiPickerMemoId by viewModel.emojiPickerMemoId.collectAsStateWithLifecycle()
    val commentSheetMemoId by viewModel.commentSheetMemoId.collectAsStateWithLifecycle()
    val commentPreviews by viewModel.commentPreviews.collectAsStateWithLifecycle()
    val creatorNames by viewModel.creatorNames.collectAsStateWithLifecycle()
    val memos = viewModel.memos.collectAsLazyPagingItems()
    val scrollToTop by viewModel.scrollToTop.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var viewerMedia by remember { mutableStateOf<ViewableMedia?>(null) }

    // Refresh list when returning from editor after saving
    LaunchedEffect(refreshOnReturn) {
        if (refreshOnReturn) {
            viewModel.refreshAndScrollToTop()
            onRefreshConsumed()
        }
    }

    // Scroll to top after paging data finishes refreshing
    LaunchedEffect(scrollToTop) {
        if (scrollToTop) {
            snapshotFlow { memos.loadState.refresh }
                .first { it is LoadState.Loading }
            snapshotFlow { memos.loadState.refresh }
                .first { it is LoadState.NotLoading }
            listState.scrollToItem(0)
            viewModel.consumeScrollToTop()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MemosDrawerContent(
                currentDestination = if (showArchived) SidebarDestination.Archived else SidebarDestination.Home,
                userName = currentUser?.let { it.nickname.ifBlank { it.username } } ?: "",
                onNavigate = { destination ->
                    scope.launch { drawerState.close() }
                    when (destination) {
                        SidebarDestination.Home -> {
                            if (showArchived) viewModel.toggleArchived()
                        }
                        SidebarDestination.Archived -> {
                            if (!showArchived) viewModel.toggleArchived()
                        }
                        else -> onNavigate(destination)
                    }
                },
                onProfileClick = {
                    scope.launch { drawerState.close() }
                    onProfileClick()
                },
            )
        },
    ) {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = { Text(if (showArchived) stringResource(UiR.string.show_archived) else stringResource(UiR.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(UiR.string.menu))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                if (!showArchived) {
                    FloatingActionButton(
                        onClick = onNewMemoClick,
                        containerColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(UiR.string.new_memo))
                    }
                }
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                )
                MemoList(
                    memos = memos,
                    activeTag = activeTag,
                    onMemoClick = onMemoClick,
                    onTagClick = { tag -> viewModel.setTagFilter(tag) },
                    onClearFilter = { viewModel.clearFilter() },
                    listState = listState,
                    serverUrl = viewModel.serverUrl,
                    creatorNames = creatorNames,
                    onReactionClick = { memoUid, type -> viewModel.toggleReaction(memoUid, type) },
                    onAddReaction = { memoUid -> viewModel.toggleEmojiPicker(memoUid) },
                    onCommentClick = { memoUid -> viewModel.openCommentSheet(memoUid) },
                    emojiPickerMemoId = emojiPickerMemoId,
                    reactionOverrides = reactionOverrides,
                    commentPreviews = commentPreviews,
                    onLoadComments = { viewModel.loadCommentPreviews(it) },
                    onRefresh = {
                        viewModel.refresh()
                        memos.refresh()
                    },
                    onMediaClick = { media -> viewerMedia = media },
                    onEditMemo = onEditMemo,
                    onArchiveMemo = { memoUid -> viewModel.archiveMemo(memoUid) },
                    onDeleteMemo = { memoUid -> viewModel.deleteMemo(memoUid) },
                )
            }
        }
    }

    commentSheetMemoId?.let { memoId ->
        CommentBottomSheet(
            onSend = { content -> viewModel.sendComment(memoId, content) },
            onDismiss = { viewModel.closeCommentSheet() },
        )
    }

    viewerMedia?.let { media ->
        MediaViewerDialog(
            media = media,
            headers = viewModel.authHeaders,
            onDismiss = { viewerMedia = null },
        )
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(UiR.string.search_memos_placeholder)) },
        singleLine = true,
        shape = MemosShapes.SearchBar,
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(UiR.string.clear))
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoList(
    memos: LazyPagingItems<Memo>,
    activeTag: String?,
    onMemoClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    serverUrl: String = "",
    showCreator: Boolean = false,
    creatorNames: Map<String, String> = emptyMap(),
    onReactionClick: ((String, String) -> Unit)? = null,
    onAddReaction: ((String) -> Unit)? = null,
    onCommentClick: ((String) -> Unit)? = null,
    emojiPickerMemoId: String? = null,
    reactionOverrides: Map<String, List<Reaction>> = emptyMap(),
    commentPreviews: Map<String, List<Memo>> = emptyMap(),
    onLoadComments: ((List<Memo>) -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    onMediaClick: ((ViewableMedia) -> Unit)? = null,
    onEditMemo: ((String) -> Unit)? = null,
    onArchiveMemo: ((String) -> Unit)? = null,
    onDeleteMemo: ((String) -> Unit)? = null,
) {
    val loadState = memos.loadState
    val isRefreshing = loadState.refresh is LoadState.Loading && memos.itemCount > 0

    if (memos.itemCount == 0 && loadState.refresh is LoadState.Loading) {
        LoadingContent(modifier = modifier)
        return
    }

    // Load comment previews for visible memos
    LaunchedEffect(memos.itemCount) {
        if (onLoadComments != null && memos.itemCount > 0) {
            val visibleMemos = (0 until memos.itemCount).mapNotNull { memos[it] }
            onLoadComments(visibleMemos)
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { onRefresh?.invoke() },
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (activeTag != null) {
                item(key = "tag_filter") {
                    AssistChip(
                        onClick = onClearFilter,
                        label = { Text("#$activeTag") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(UiR.string.clear_filter),
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                    )
                }
            }

            items(
                count = memos.itemCount,
                key = { index -> memos[index]?.uid ?: index },
            ) { index ->
                val memo = memos[index] ?: return@items
                MemoCard(
                    memo = memo,
                    onClick = { onMemoClick(memo.uid) },
                    serverUrl = serverUrl,
                    onTagClick = onTagClick,
                    creatorLabel = if (showCreator) creatorNames[memo.creator] ?: memo.creator.substringAfterLast("/") else null,
                    onReactionClick = onReactionClick?.let { callback -> { type -> callback(memo.uid, type) } },
                    onAddReaction = onAddReaction?.let { callback -> { callback(memo.uid) } },
                    onCommentClick = onCommentClick?.let { callback -> { callback(memo.uid) } },
                    showEmojiPicker = emojiPickerMemoId == memo.uid,
                    reactionOverrides = reactionOverrides[memo.uid],
                    commentPreviews = commentPreviews[memo.uid] ?: emptyList(),
                    creatorNames = creatorNames,
                    onMediaClick = onMediaClick,
                    onEdit = onEditMemo?.let { callback -> { callback(memo.uid) } },
                    onArchive = onArchiveMemo?.let { callback -> { callback(memo.uid) } },
                    onDelete = onDeleteMemo?.let { callback -> { callback(memo.uid) } },
                    contentRenderer = { content ->
                        MarkdownPreview(content = content, maxLines = 6)
                    },
                )
            }

            if (loadState.append is LoadState.Loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    onSend: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var text by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(UiR.string.write_comment_placeholder)) },
                singleLine = false,
                maxLines = 3,
                shape = MemosShapes.Input,
            )
            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSend(text.trim())
                    }
                },
                enabled = text.isNotBlank(),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(UiR.string.send_comment),
                    tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
