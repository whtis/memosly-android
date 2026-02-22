package com.whtis.memosly.feature.explore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.whtis.memosly.core.ui.R as UiR
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.first
import com.whtis.memosly.core.ui.component.MediaViewerDialog
import com.whtis.memosly.core.ui.component.ViewableMedia
import com.whtis.memosly.feature.home.CommentBottomSheet
import com.whtis.memosly.feature.home.MemoList
import com.whtis.memosly.feature.home.SearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExploreScreen(
    onBack: (() -> Unit)?,
    onMemoClick: (String) -> Unit,
    onEditMemo: (String) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val creatorNames by viewModel.creatorNames.collectAsStateWithLifecycle()
    val reactionOverrides by viewModel.reactionOverrides.collectAsStateWithLifecycle()
    val emojiPickerMemoId by viewModel.emojiPickerMemoId.collectAsStateWithLifecycle()
    val commentSheetMemoId by viewModel.commentSheetMemoId.collectAsStateWithLifecycle()
    val commentPreviews by viewModel.commentPreviews.collectAsStateWithLifecycle()
    val memos = viewModel.memos.collectAsLazyPagingItems()
    val scrollToTop by viewModel.scrollToTop.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var viewerMedia by remember { mutableStateOf<ViewableMedia?>(null) }

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

    // Resolve creator names for loaded memos
    LaunchedEffect(memos.itemCount) {
        val creators = (0 until memos.itemCount).mapNotNull { memos[it]?.creator }
        viewModel.resolveCreators(creators)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(UiR.string.explore)) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(UiR.string.back),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
            )
            MemoList(
                memos = memos,
                activeTag = null,
                onMemoClick = onMemoClick,
                onTagClick = {},
                onClearFilter = {},
                listState = listState,
                serverUrl = viewModel.serverUrl,
                showCreator = true,
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
