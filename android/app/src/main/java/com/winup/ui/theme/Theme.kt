package com.winup.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkScheme = darkColorScheme(
    primary = Neon,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkContainer,
    onPrimaryContainer = DarkOnContainer,
    secondary = Orange,
    onSecondary = DarkOnPrimary,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkMuted,
    outline = DarkBorder,
    error = DangerDark,
    onError = DarkOnDanger,
)

private val LightScheme = lightColorScheme(
    primary = Neon,
    onPrimary = LightText,
    primaryContainer = NeonSoft,
    onPrimaryContainer = SuccessLight,
    secondary = Orange,
    onSecondary = LightText,
    background = LightBackground,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightMuted,
    outline = LightBorder,
    error = DangerLight,
    onError = LightSurface,
)

@Composable
fun WinUpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme
    val extras = if (darkTheme) DarkWinUpColors else LightWinUpColors

    CompositionLocalProvider(LocalWinUpColors provides extras) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WinUpTypography,
            content = content,
        )
    }
}
