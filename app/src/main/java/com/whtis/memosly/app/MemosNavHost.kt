package com.whtis.memosly.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import com.whtis.memosly.core.network.SessionPreferences
import com.whtis.memosly.core.ui.component.SidebarDestination
import com.whtis.memosly.feature.auth.AUTH_ROUTE
import com.whtis.memosly.feature.auth.authScreen
import com.whtis.memosly.feature.auth.navigateToAuth
import com.whtis.memosly.feature.home.HOME_ROUTE
import com.whtis.memosly.feature.home.homeScreen
import com.whtis.memosly.feature.home.navigateToHome
import com.whtis.memosly.feature.memo.memoDetailScreen
import com.whtis.memosly.feature.memo.memoEditorScreen
import com.whtis.memosly.feature.memo.navigateToMemoDetail
import com.whtis.memosly.feature.memo.navigateToMemoEditor
import com.whtis.memosly.feature.notifications.navigateToNotifications
import com.whtis.memosly.feature.notifications.notificationsScreen
import com.whtis.memosly.feature.profile.navigateToProfile
import com.whtis.memosly.feature.profile.profileScreen
import com.whtis.memosly.feature.explore.exploreScreen
import com.whtis.memosly.feature.explore.navigateToExplore

private const val MAIN_TABS_ROUTE = "main_tabs"

@Composable
fun MemosNavHost(
    sessionPreferences: SessionPreferences,
<<<<<<< HEAD
    sharedText: String? = null,
    onSharedTextConsumed: () -> Unit = {},
=======
    sharedTextState: MutableState<String?> = mutableStateOf(null),
>>>>>>> origin/night-shift/20260514-share-intent
) {
    val navController = rememberNavController()
    val navMode by sessionPreferences.navModeFlow.collectAsStateWithLifecycle()

    // React to nav mode changes immediately (skip initial value)
    LaunchedEffect(Unit) {
        snapshotFlow { navMode }
            .drop(1)
            .collect { mode ->
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (currentRoute == AUTH_ROUTE) return@collect
                when (mode) {
                    SessionPreferences.NAV_MODE_TABS -> {
                        navController.navigate(MAIN_TABS_ROUTE) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                    else -> {
                        navController.navigateToHome()
                    }
                }
            }
    }

<<<<<<< HEAD
    // Open editor with shared text when share intent arrives after auth
    LaunchedEffect(sharedText) {
        val text = sharedText ?: return@LaunchedEffect
        if (text.isBlank()) return@LaunchedEffect
        snapshotFlow { navController.currentBackStackEntry?.destination?.route }
            .first { it != null && it != AUTH_ROUTE }
        navController.navigateToMemoEditor(sharedText = text)
        onSharedTextConsumed()
=======
    // Consume shared text once the user is past auth, then clear it so it doesn't fire again
    // on configuration changes or recompositions.
    val pendingSharedText = sharedTextState.value
    LaunchedEffect(pendingSharedText) {
        if (pendingSharedText.isNullOrEmpty()) return@LaunchedEffect
        navController.currentBackStackEntryFlow.first { entry ->
            entry.destination.route != AUTH_ROUTE
        }
        navController.navigateToMemoEditor(sharedText = pendingSharedText)
        sharedTextState.value = null
>>>>>>> origin/night-shift/20260514-share-intent
    }

    NavHost(
        navController = navController,
        startDestination = AUTH_ROUTE,
    ) {
        authScreen(
            onLoginSuccess = {
                if (navMode == SessionPreferences.NAV_MODE_TABS) {
                    navController.navigate(MAIN_TABS_ROUTE) {
                        popUpTo(0) { inclusive = true }
                    }
                } else {
                    navController.navigateToHome()
                }
            },
        )

        // Drawer mode: home with sidebar
        homeScreen(
            onMemoClick = { name -> navController.navigateToMemoDetail(name) },
            onNewMemoClick = { navController.navigateToMemoEditor() },
            onNavigate = { destination ->
                when (destination) {
                    SidebarDestination.Home -> {}
                    SidebarDestination.Archived -> {}
                    SidebarDestination.Explore -> navController.navigateToExplore()
                    SidebarDestination.Notifications -> navController.navigateToNotifications()
                }
            },
            onProfileClick = { navController.navigateToProfile() },
            onEditMemo = { name -> navController.navigateToMemoEditor(name) },
        )

        // Bottom tab mode: all-in-one shell
        composable(route = MAIN_TABS_ROUTE) { backStackEntry ->
            val memoSaved by backStackEntry.savedStateHandle
                .getStateFlow("memo_saved", false)
                .collectAsStateWithLifecycle()
            val memoSavedPublic by backStackEntry.savedStateHandle
                .getStateFlow("memo_saved_public", false)
                .collectAsStateWithLifecycle()

            BottomTabShell(
                onMemoClick = { name -> navController.navigateToMemoDetail(name) },
                onNewMemoClick = { navController.navigateToMemoEditor() },
                onSignOut = { navController.navigateToAuth() },
                onEditMemo = { name -> navController.navigateToMemoEditor(name) },
                refreshOnReturn = memoSaved,
                savedPublic = memoSavedPublic,
                onRefreshConsumed = {
                    backStackEntry.savedStateHandle["memo_saved"] = false
                    backStackEntry.savedStateHandle["memo_saved_public"] = false
                },
            )
        }

        // Shared routes (work from both modes)
        memoDetailScreen(
            onBack = { navController.popBackStack() },
            onEdit = { name -> navController.navigateToMemoEditor(name) },
            onTagClick = { tag -> navController.navigateToHome(tag) },
            onMemoChanged = {
                navController.previousBackStackEntry?.savedStateHandle?.set("memo_saved", true)
                navController.popBackStack()
            },
        )

        memoEditorScreen(
            onBack = { navController.popBackStack() },
            onSaved = { isPublic ->
                navController.previousBackStackEntry?.savedStateHandle?.set("memo_saved", true)
                navController.previousBackStackEntry?.savedStateHandle?.set("memo_saved_public", isPublic)
                navController.popBackStack()
            },
        )

        exploreScreen(
            onBack = { navController.popBackStack() },
            onMemoClick = { name -> navController.navigateToMemoDetail(name) },
            onEditMemo = { name -> navController.navigateToMemoEditor(name) },
        )

        profileScreen(
            onBack = { navController.popBackStack() },
            onSignOut = { navController.navigateToAuth() },
        )

        notificationsScreen(
            onBack = { navController.popBackStack() },
        )
    }
}
