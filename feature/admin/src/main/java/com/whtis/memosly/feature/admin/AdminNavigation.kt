package com.whtis.memosly.feature.admin

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val ADMIN_ROUTE = "admin"

fun NavController.navigateToAdmin() {
    navigate(ADMIN_ROUTE)
}

fun NavGraphBuilder.adminScreen(
    onBack: () -> Unit,
) {
    composable(route = ADMIN_ROUTE) {
        AdminScreen(onBack = onBack)
    }
}
