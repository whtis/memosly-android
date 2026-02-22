package com.whtis.memosly.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.whtis.memosly.core.ui.R as UiR
import com.whtis.memosly.feature.explore.ExploreTabContent
import com.whtis.memosly.feature.explore.ExploreViewModel
import com.whtis.memosly.feature.home.HomeTabContent
import com.whtis.memosly.feature.home.HomeViewModel
import com.whtis.memosly.feature.profile.ProfileTabContent

private enum class Tab(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelResId: Int,
) {
    Home(Icons.Filled.Home, Icons.Outlined.Home, UiR.string.tab_home),
    Explore(Icons.Filled.Explore, Icons.Outlined.Explore, UiR.string.tab_explore),
    Archive(Icons.Filled.Archive, Icons.Outlined.Archive, UiR.string.tab_archive),
    Me(Icons.Filled.Person, Icons.Outlined.Person, UiR.string.tab_me),
}

@Composable
fun BottomTabShell(
    onMemoClick: (String) -> Unit,
    onNewMemoClick: () -> Unit,
    onSignOut: () -> Unit,
    onEditMemo: (String) -> Unit = {},
    refreshOnReturn: Boolean = false,
    savedPublic: Boolean = false,
    onRefreshConsumed: () -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val homeViewModel: HomeViewModel = hiltViewModel()
    val exploreViewModel: ExploreViewModel = hiltViewModel()
    val homeListState = rememberLazyListState()
    val exploreListState = rememberLazyListState()

    LaunchedEffect(selectedTab) {
        homeViewModel.setArchived(selectedTab == Tab.Archive.ordinal)
    }

    // Refresh lists when returning from editor after saving
    LaunchedEffect(refreshOnReturn) {
        if (refreshOnReturn) {
            if (savedPublic) {
                selectedTab = Tab.Explore.ordinal
                homeViewModel.refresh()
                exploreViewModel.refreshAndScrollToTop()
            } else {
                selectedTab = Tab.Home.ordinal
                homeViewModel.refreshAndScrollToTop()
                exploreViewModel.refresh()
            }
            onRefreshConsumed()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                Tab.Home.ordinal -> HomeTabContent(
                    title = stringResource(UiR.string.app_name),
                    onMemoClick = onMemoClick,
                    onEditMemo = onEditMemo,
                    listState = homeListState,
                    viewModel = homeViewModel,
                )
                Tab.Explore.ordinal -> ExploreTabContent(
                    onMemoClick = onMemoClick,
                    onEditMemo = onEditMemo,
                    listState = exploreListState,
                    viewModel = exploreViewModel,
                )
                Tab.Archive.ordinal -> HomeTabContent(
                    title = stringResource(UiR.string.tab_archive),
                    onMemoClick = onMemoClick,
                    onEditMemo = onEditMemo,
                    listState = homeListState,
                    viewModel = homeViewModel,
                )
                Tab.Me.ordinal -> ProfileTabContent(
                    onSignOut = onSignOut,
                )
            }
        }

        // Bottom bar with center FAB
        Box {
            NavigationBar {
                val tabs = Tab.entries
                tabs.forEachIndexed { index, tab ->
                    if (index == tabs.size / 2) {
                        // Spacer for center FAB
                        NavigationBarItem(
                            selected = false,
                            onClick = onNewMemoClick,
                            icon = {
                                // Invisible placeholder — FAB is overlaid
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surface,
                                )
                            },
                            label = {},
                            enabled = false,
                        )
                    }
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                if (selectedTab == index) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = stringResource(tab.labelResId),
                            )
                        },
                        label = { Text(stringResource(tab.labelResId)) },
                    )
                }
            }

            // Center FAB overlay
            FloatingActionButton(
                onClick = onNewMemoClick,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-16).dp)
                    .size(48.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                ),
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(UiR.string.new_memo),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
