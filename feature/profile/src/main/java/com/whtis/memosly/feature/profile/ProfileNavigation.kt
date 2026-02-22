package com.whtis.memosly.feature.profile

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val PROFILE_ROUTE = "profile"

fun NavController.navigateToProfile() {
    navigate(PROFILE_ROUTE)
}

@Composable
fun ProfileTabContent(
    onSignOut: () -> Unit,
) {
    ProfileScreen(onBack = null, onSignOut = onSignOut)
}

fun NavGraphBuilder.profileScreen(
    onBack: (() -> Unit)?,
    onSignOut: () -> Unit,
) {
    composable(route = PROFILE_ROUTE) {
        ProfileScreen(onBack = onBack, onSignOut = onSignOut)
    }
}
