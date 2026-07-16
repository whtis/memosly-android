package com.whtis.memosly.feature.memo

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

const val MEMO_DETAIL_ROUTE = "memo/{memoId}"
const val MEMO_EDITOR_ROUTE = "memo/editor?memoId={memoId}&sharedText={sharedText}&sharedMedia={sharedMedia}"

fun NavController.navigateToMemoDetail(memoId: String) {
    navigate("memo/$memoId")
}

/**
 * [sharedMedia] only signals that a share is waiting — the URIs themselves travel through
 * SharedMediaBuffer, since they arrive in batches and can be too long for a route string.
 */
fun NavController.navigateToMemoEditor(
    memoId: String? = null,
    sharedText: String? = null,
    sharedMedia: Boolean = false,
) {
    val params = buildList {
        if (memoId != null) add("memoId=$memoId")
        if (!sharedText.isNullOrBlank()) {
            add("sharedText=${Uri.encode(sharedText)}")
        }
        if (sharedMedia) add("sharedMedia=true")
    }
    val route = if (params.isEmpty()) "memo/editor" else "memo/editor?${params.joinToString("&")}"
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
            navArgument("sharedMedia") {
                type = NavType.BoolType
                defaultValue = false
            },
        ),
    ) {
        MemoEditorScreen(
            onBack = onBack,
            onSaved = onSaved,
        )
    }
}
