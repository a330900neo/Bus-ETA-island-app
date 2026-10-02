package com.busetaisland.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkBusColors.primary,
    onPrimary = DarkBusColors.onPrimary,
    primaryContainer = DarkBusColors.primaryContainer,
    onPrimaryContainer = DarkBusColors.onPrimaryContainer,
    secondary = DarkBusColors.emeraldGreen,
    onSecondary = DarkBusColors.onPrimary,
    secondaryContainer = DarkBusColors.emeraldContainer,
    onSecondaryContainer = DarkBusColors.emeraldGreen,
    tertiary = DarkBusColors.primary,
    onTertiary = DarkBusColors.onPrimary,
    background = DarkBusColors.background,
    onBackground = DarkBusColors.textPrimary,
    surface = DarkBusColors.surface,
    onSurface = DarkBusColors.textPrimary,
    surfaceVariant = DarkBusColors.surfaceVariant,
    onSurfaceVariant = DarkBusColors.textSecondary,
    surfaceContainer = DarkBusColors.surface,
    surfaceContainerHigh = DarkBusColors.surfaceVariant,
    surfaceContainerHighest = Color(0xFF5A5660),
    surfaceContainerLow = DarkBusColors.surfaceElevated,
    outline = DarkBusColors.cardBorder,
    outlineVariant = DarkBusColors.subtleBorder,
    error = DarkBusColors.roseAlert,
    onError = Color(0xFF690005),
    errorContainer = DarkBusColors.roseContainer,
    onErrorContainer = DarkBusColors.roseAlert
)

private val LightColorScheme = lightColorScheme(
    primary = LightBusColors.primary,
    onPrimary = LightBusColors.onPrimary,
    primaryContainer = LightBusColors.primaryContainer,
    onPrimaryContainer = LightBusColors.onPrimaryContainer,
    secondary = LightBusColors.emeraldGreen,
    onSecondary = LightBusColors.onPrimary,
    secondaryContainer = LightBusColors.emeraldContainer,
    onSecondaryContainer = LightBusColors.emeraldGreen,
    tertiary = LightBusColors.primary,
    onTertiary = LightBusColors.onPrimary,
    background = LightBusColors.background,
    onBackground = LightBusColors.textPrimary,
    surface = LightBusColors.surface,
    onSurface = LightBusColors.textPrimary,
    surfaceVariant = LightBusColors.surfaceVariant,
    onSurfaceVariant = LightBusColors.textSecondary,
    surfaceContainer = LightBusColors.surface,
    surfaceContainerHigh = LightBusColors.surfaceVariant,
    surfaceContainerHighest = Color(0xFFE1E3EA),
    surfaceContainerLow = LightBusColors.surfaceElevated,
    outline = LightBusColors.cardBorder,
    outlineVariant = LightBusColors.subtleBorder,
    error = LightBusColors.roseAlert,
    onError = Color(0xFFFFFFFF),
    errorContainer = LightBusColors.roseContainer,
    onErrorContainer = LightBusColors.roseAlert
)

@Composable
fun BusEtaTheme(
    darkTheme: Boolean = true,
    appTheme: String = "dark",
    customColors: BusColors? = null,
    content: @Composable () -> Unit
) {
    val busColors = when {
        appTheme == "custom" && customColors != null -> customColors
        !darkTheme -> LightBusColors
        else -> DarkBusColors
    }
    val colorScheme = if (!darkTheme && appTheme != "custom") LightColorScheme else DarkColorScheme

    CompositionLocalProvider(LocalBusColors provides busColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    BusEtaTheme(darkTheme = darkTheme, content = content)
}
