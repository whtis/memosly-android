package com.whtis.memosly.feature.explore

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val EXPLORE_ROUTE = "explore"

fun NavController.navigateToExplore() {
    navigate(EXPLORE_ROUTE)
}

@Composable
fun ExploreTabContent(
    onMemoClick: (String) -> Unit,
    onEditMemo: (String) -> Unit = {},
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    viewModel: ExploreViewModel? = null,
) {
    ExploreScreen(onBack = null, onMemoClick = onMemoClick, onEditMemo = onEditMemo, listState = listState, viewModel = viewModel ?: hiltViewModel())
}

fun NavGraphBuilder.exploreScreen(
    onBack: (() -> Unit)?,
    onMemoClick: (String) -> Unit,
    onEditMemo: (String) -> Unit = {},
) {
    composable(route = EXPLORE_ROUTE) {
        ExploreScreen(onBack = onBack, onMemoClick = onMemoClick, onEditMemo = onEditMemo)
    }
}
