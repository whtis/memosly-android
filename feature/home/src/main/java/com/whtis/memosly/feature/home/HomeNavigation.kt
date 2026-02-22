package com.whtis.memosly.feature.home

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import com.whtis.memosly.core.ui.component.MediaViewerDialog
import com.whtis.memosly.core.ui.component.ViewableMedia
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.paging.compose.collectAsLazyPagingItems
import com.whtis.memosly.core.ui.component.SidebarDestination
import kotlinx.coroutines.flow.first

const val HOME_ROUTE = "home"

fun NavController.navigateToHome(tag: String? = null) {
    val route = if (tag != null) "home?tag=${Uri.encode(tag)}" else HOME_ROUTE
    navigate(route) {
        popUpTo(0) { inclusive = true }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTabContent(
    title: String,
    onMemoClick: (String) -> Unit,
    onEditMemo: (String) -> Unit = {},
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val activeTag by viewModel.activeTag.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val reactionOverrides by viewModel.reactionOverrides.collectAsStateWithLifecycle()
    val emojiPickerMemoId by viewModel.emojiPickerMemoId.collectAsStateWithLifecycle()
    val commentSheetMemoId by viewModel.commentSheetMemoId.collectAsStateWithLifecycle()
    val commentPreviews by viewModel.commentPreviews.collectAsStateWithLifecycle()
    val creatorNames by viewModel.creatorNames.collectAsStateWithLifecycle()
    val memos = viewModel.memos.collectAsLazyPagingItems()
    val scrollToTop by viewModel.scrollToTop.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var viewerMedia by remember { mutableStateOf<ViewableMedia?>(null) }

    // Scroll to top after paging data finishes refreshing
    LaunchedEffect(scrollToTop) {
        if (scrollToTop) {
            // Wait for refresh to start loading
            snapshotFlow { memos.loadState.refresh }
                .first { it is LoadState.Loading }
            // Wait for refresh to finish
            snapshotFlow { memos.loadState.refresh }
                .first { it is LoadState.NotLoading }
            listState.scrollToItem(0)
            viewModel.consumeScrollToTop()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(title) },
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

fun NavGraphBuilder.homeScreen(
    onMemoClick: (String) -> Unit,
    onNewMemoClick: () -> Unit,
    onNavigate: (SidebarDestination) -> Unit,
    onProfileClick: () -> Unit,
    onEditMemo: (String) -> Unit = {},
) {
    composable(
        route = "home?tag={tag}",
        arguments = listOf(
            navArgument("tag") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) { backStackEntry ->
        val memoSaved by backStackEntry.savedStateHandle
            .getStateFlow("memo_saved", false)
            .collectAsStateWithLifecycle()

        HomeScreen(
            onMemoClick = onMemoClick,
            onNewMemoClick = onNewMemoClick,
            onNavigate = onNavigate,
            onProfileClick = onProfileClick,
            onEditMemo = onEditMemo,
            refreshOnReturn = memoSaved,
            onRefreshConsumed = { backStackEntry.savedStateHandle["memo_saved"] = false },
        )
    }
}
