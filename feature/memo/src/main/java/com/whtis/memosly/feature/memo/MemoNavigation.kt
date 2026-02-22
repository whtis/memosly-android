package com.whtis.memosly.feature.memo

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

const val MEMO_DETAIL_ROUTE = "memo/{memoId}"
const val MEMO_EDITOR_ROUTE = "memo/editor?memoId={memoId}"

fun NavController.navigateToMemoDetail(memoId: String) {
    navigate("memo/$memoId")
}

fun NavController.navigateToMemoEditor(memoId: String? = null) {
    val route = if (memoId != null) "memo/editor?memoId=$memoId" else "memo/editor"
    navigate(route)
}

fun NavGraphBuilder.memoDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onMemoChanged: () -> Unit = onBack,
) {
    composable(
        route = MEMO_DETAIL_ROUTE,
        arguments = listOf(navArgument("memoId") { type = NavType.StringType }),
    ) {
        MemoDetailScreen(
            onBack = onBack,
            onEdit = onEdit,
            onTagClick = onTagClick,
            onMemoChanged = onMemoChanged,
        )
    }
}

fun NavGraphBuilder.memoEditorScreen(
    onBack: () -> Unit,
    onSaved: (isPublic: Boolean) -> Unit,
) {
    composable(
        route = MEMO_EDITOR_ROUTE,
        arguments = listOf(
            navArgument("memoId") {
                type = NavType.StringType
                defaultValue = ""
            },
        ),
    ) {
        MemoEditorScreen(
            onBack = onBack,
            onSaved = onSaved,
        )
    }
}
