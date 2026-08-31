package com.busetaisland.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BusLavenderPrimary,
    onPrimary = BusLavenderOnPrimary,
    primaryContainer = BusLavenderContainer,
    onPrimaryContainer = BusLavenderOnContainer,
    secondary = BusEmeraldGreen,
    onSecondary = BusLavenderOnPrimary,
    secondaryContainer = BusEmeraldContainer,
    onSecondaryContainer = BusEmeraldGreen,
    tertiary = BusLavenderPrimary,
    onTertiary = BusLavenderOnPrimary,
    background = BusDarkBackground,
    onBackground = BusTextPrimary,
    surface = BusDarkSurface,
    onSurface = BusTextPrimary,
    surfaceVariant = BusDarkSurfaceVariant,
    onSurfaceVariant = BusTextSecondary,
    surfaceContainer = BusDarkSurface,
    surfaceContainerHigh = BusDarkSurfaceVariant,
    surfaceContainerHighest = Color(0xFF5A5660),
    surfaceContainerLow = BusDarkSurfaceElevated,
    outline = BusCardBorder,
    outlineVariant = BusSubtleBorder,
    error = BusRoseAlert,
    onError = Color(0xFF690005),
    errorContainer = BusRoseContainer,
    onErrorContainer = BusRoseAlert
)

@Composable
fun BusEtaTheme(
    darkTheme: Boolean = true, // Default to dark theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    BusEtaTheme(darkTheme = darkTheme, content = content)
}
