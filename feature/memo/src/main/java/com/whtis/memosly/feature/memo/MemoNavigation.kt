package com.whtis.memosly.feature.memo

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import java.net.URLEncoder

const val MEMO_DETAIL_ROUTE = "memo/{memoId}"
const val MEMO_EDITOR_ROUTE = "memo/editor?memoId={memoId}&sharedText={sharedText}"

fun NavController.navigateToMemoDetail(memoId: String) {
    navigate("memo/$memoId")
}

fun NavController.navigateToMemoEditor(memoId: String? = null, sharedText: String? = null) {
    val params = buildList {
        if (memoId != null) add("memoId=$memoId")
        if (!sharedText.isNullOrEmpty()) {
            // URI uses %20 for space; URLEncoder emits '+' (form encoding) so swap it.
            add("sharedText=" + URLEncoder.encode(sharedText, "UTF-8").replace("+", "%20"))
        }
    }
    val route = if (params.isEmpty()) "memo/editor" else "memo/editor?" + params.joinToString("&")
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
            navArgument("sharedText") {
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
