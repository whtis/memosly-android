package com.whtis.memosly.feature.search

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val SEARCH_ROUTE = "search"

fun NavController.navigateToSearch() {
    navigate(SEARCH_ROUTE)
}

@Composable
fun SearchTabContent(
    onMemoClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
) {
    SearchScreen(onBack = null, onMemoClick = onMemoClick, onTagClick = onTagClick)
}

fun NavGraphBuilder.searchScreen(
    onBack: (() -> Unit)?,
    onMemoClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
) {
    composable(route = SEARCH_ROUTE) {
        SearchScreen(onBack = onBack, onMemoClick = onMemoClick, onTagClick = onTagClick)
    }
}
