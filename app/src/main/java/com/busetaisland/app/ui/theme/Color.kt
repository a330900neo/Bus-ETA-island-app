package com.busetaisland.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class BusColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val cardBorder: Color,
    val subtleBorder: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val emeraldGreen: Color,
    val emeraldContainer: Color,
    val roseAlert: Color,
    val roseContainer: Color,
    val amberWarning: Color,
    val islandBlack: Color = Color(0xFF000000)
)

val DarkBusColors = BusColors(
    background = Color(0xFF1A1C1E),
    surface = Color(0xFF2E3032),
    surfaceElevated = Color(0xFF212325),
    surfaceVariant = Color(0xFF49454F),
    textPrimary = Color(0xFFE2E2E6),
    textSecondary = Color(0xFFC4C6D0),
    textMuted = Color(0xFF938F99),
    cardBorder = Color(0xFF3F4143),
    subtleBorder = Color(0x1FFFFFFF),
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    emeraldGreen = Color(0xFF86F8B6),
    emeraldContainer = Color(0xFF005234),
    roseAlert = Color(0xFFFFB4AB),
    roseContainer = Color(0xFF690005),
    amberWarning = Color(0xFFFFD56B)
)

val LightBusColors = BusColors(
    background = Color(0xFFF4F5F9),        // Clean off-white background canvas
    surface = Color(0xFFFFFFFF),           // Pure white card surfaces
    surfaceElevated = Color(0xFFEAECEF),   // Slightly darker elevated surface for contrast
    surfaceVariant = Color(0xFFE1E3EA),    // Input fields & containers
    textPrimary = Color(0xFF191C1E),       // High contrast dark text
    textSecondary = Color(0xFF44474E),     // Medium contrast text
    textMuted = Color(0xFF74777F),         // Muted text / icons
    cardBorder = Color(0xFFE0E2E8),        // Subtle light card border
    subtleBorder = Color(0x1F000000),      // Black 12% border
    primary = Color(0xFF6750A4),           // Deep lavender / violet for primary actions on light background
    onPrimary = Color(0xFFFFFFFF),         // White text on primary buttons
    primaryContainer = Color(0xFFEADDFF),  // Soft lavender container
    onPrimaryContainer = Color(0xFF21005D),// Dark purple on lavender container
    emeraldGreen = Color(0xFF006C4C),      // Deep green for readable status text/accents on light
    emeraldContainer = Color(0xFFC8F8D8),  // Soft green container
    roseAlert = Color(0xFFBA1A1A),         // Readable red alert for light background
    roseContainer = Color(0xFFFFDAD6),     // Light red container
    amberWarning = Color(0xFF7D5200)       // Dark amber for light background contrast
)

val LocalBusColors = staticCompositionLocalOf { DarkBusColors }

// Dynamic theme accessors for backward compatibility and seamless theme switching
val BusDarkBackground: Color
    @Composable get() = LocalBusColors.current.background

val BusDarkSurface: Color
    @Composable get() = LocalBusColors.current.surface

val BusDarkSurfaceVariant: Color
    @Composable get() = LocalBusColors.current.surfaceVariant

val BusDarkSurfaceElevated: Color
    @Composable get() = LocalBusColors.current.surfaceElevated

val BusIslandBlack: Color
    @Composable get() = LocalBusColors.current.islandBlack

val BusLavenderPrimary: Color
    @Composable get() = LocalBusColors.current.primary

val BusLavenderOnPrimary: Color
    @Composable get() = LocalBusColors.current.onPrimary

val BusLavenderContainer: Color
    @Composable get() = LocalBusColors.current.primaryContainer

val BusLavenderOnContainer: Color
    @Composable get() = LocalBusColors.current.onPrimaryContainer

val BusEmeraldGreen: Color
    @Composable get() = LocalBusColors.current.emeraldGreen

val BusEmeraldContainer: Color
    @Composable get() = LocalBusColors.current.emeraldContainer

val BusAmberWarning: Color
    @Composable get() = LocalBusColors.current.amberWarning

val BusRoseAlert: Color
    @Composable get() = LocalBusColors.current.roseAlert

val BusRoseContainer: Color
    @Composable get() = LocalBusColors.current.roseContainer

val BusIndigoAccent: Color
    @Composable get() = LocalBusColors.current.primary

val BusTextPrimary: Color
    @Composable get() = LocalBusColors.current.textPrimary

val BusTextSecondary: Color
    @Composable get() = LocalBusColors.current.textSecondary

val BusTextMuted: Color
    @Composable get() = LocalBusColors.current.textMuted

val BusCardBorder: Color
    @Composable get() = LocalBusColors.current.cardBorder

val BusSubtleBorder: Color
    @Composable get() = LocalBusColors.current.subtleBorder

val BusIslandRing: Color
    @Composable get() = LocalBusColors.current.subtleBorder
