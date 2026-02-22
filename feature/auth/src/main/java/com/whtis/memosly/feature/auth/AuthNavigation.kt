package com.whtis.memosly.feature.auth

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val AUTH_ROUTE = "auth"

fun NavController.navigateToAuth() {
    navigate(AUTH_ROUTE) {
        popUpTo(0) { inclusive = true }
    }
}

fun NavGraphBuilder.authScreen(
    onLoginSuccess: () -> Unit,
) {
    composable(route = AUTH_ROUTE) {
        AuthScreen(onLoginSuccess = onLoginSuccess)
    }
}
