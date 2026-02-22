package com.whtis.memosly.feature.tags

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val TAGS_ROUTE = "tags"

fun NavController.navigateToTags() {
    navigate(TAGS_ROUTE)
}

fun NavGraphBuilder.tagsScreen(
    onBack: () -> Unit,
    onTagClick: (String) -> Unit,
) {
    composable(route = TAGS_ROUTE) {
        TagsScreen(onBack = onBack, onTagClick = onTagClick)
    }
}
