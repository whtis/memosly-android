package com.whtis.memosly.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = NotionBlue,
    onPrimary = NotionWhite,
    primaryContainer = NotionBlueLight,
    onPrimaryContainer = NotionBlack,
    secondary = NotionGray,
    onSecondary = NotionWhite,
    secondaryContainer = NotionGrayLight,
    onSecondaryContainer = NotionBlack,
    background = NotionWhite,
    onBackground = NotionBlack,
    surface = NotionWhite,
    onSurface = NotionBlack,
    surfaceVariant = NotionSidebar,
    onSurfaceVariant = NotionGray,
    surfaceContainerLowest = NotionWhite,
    surfaceContainerLow = NotionGrayLighter,
    surfaceContainer = NotionSurfaceContainer,
    surfaceContainerHigh = NotionGrayLight,
    surfaceContainerHighest = NotionSidebar,
    outline = NotionBorder,
    outlineVariant = NotionBorder,
    error = NotionRed,
)

private val DarkColorScheme = darkColorScheme(
    primary = NotionBlue,
    onPrimary = NotionWhite,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = NotionWhite,
    secondary = DarkGray,
    onSecondary = DarkOnSurface,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkOnSurface,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSidebar,
    onSurfaceVariant = DarkGray,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceVariant,
    surfaceContainerHighest = DarkSurfaceVariant,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = NotionRed,
)

@Composable
fun MemosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MemosTypography,
        content = content,
    )
}
