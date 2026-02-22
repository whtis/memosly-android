package com.whtis.memosly.feature.notifications

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val NOTIFICATIONS_ROUTE = "notifications"

fun NavController.navigateToNotifications() {
    navigate(NOTIFICATIONS_ROUTE)
}

fun NavGraphBuilder.notificationsScreen(
    onBack: () -> Unit,
) {
    composable(route = NOTIFICATIONS_ROUTE) {
        NotificationsScreen(onBack = onBack)
    }
}
