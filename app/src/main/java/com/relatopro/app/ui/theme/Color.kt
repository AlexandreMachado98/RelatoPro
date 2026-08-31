package com.relatopro.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand Colors (From UI Kit)
val PrimaryBlue = Color(0xFF2563EB)
val PrimaryBlueDark = Color(0xFF3B82F6)
val SidebarDark = Color(0xFF0B2A5B)
val PrimaryDark = Color(0xFF0B2A5B)
val CyanAccent = Color(0xFF06B6D4)

// Light Theme Tokens
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightCard = Color(0xFFFFFFFF)
val LightSidebar = Color(0xFF0B2A5B)
val LightBorder = Color(0xFFE2E8F0)
val LightTextPrimary = Color(0xFF0B2A5B)
val LightTextSecondary = Color(0xFF64748B)
val LightTextMuted = Color(0xFF94A3B8)
val LightInputBg = Color(0xFFFFFFFF)

// Dark Theme Tokens (Professional Slate/Navy Hierarchy)
val DarkBackground = Color(0xFF0B132B)      // Deep rich navy dark
val DarkSurface = Color(0xFF131D3F)         // Slightly elevated navy surface
val DarkSurfaceVariant = Color(0xFF1C2A58)  // Highlighted card/row surface
val DarkCard = Color(0xFF15224A)            // Card background with subtle contrast
val DarkSidebar = Color(0xFF091024)         // Ultra dark sidebar
val DarkBorder = Color(0xFF23356D)          // Subtle border
val DarkTextPrimary = Color(0xFFF1F5F9)     // High-contrast clean white/slate
val DarkTextSecondary = Color(0xFF94A3B8)   // Readable slate gray
val DarkTextMuted = Color(0xFF64748B)       // Dimmed slate
val DarkInputBg = Color(0xFF182652)         // Distinct input box background

// Status Colors (Calibrated for both Light and Dark)
val StatusConforme = Color(0xFF22C55E)
val StatusConformeDark = Color(0xFF4ADE80)
val StatusNaoConforme = Color(0xFFEF4444)
val StatusNaoConformeDark = Color(0xFFF87171)
val StatusNaoAplicavel = Color(0xFF94A3B8)
val StatusNaoAplicavelDark = Color(0xFF64748B)
val StatusWarning = Color(0xFFF59E0B)
val StatusWarningDark = Color(0xFFFBBF24)

// Backward compatibility references (Default to Light)
val BackgroundLight = LightBackground
val SurfaceWhite = LightSurface
val BorderColor = LightBorder
val TextPrimary = LightTextPrimary
val TextSecondary = LightTextSecondary

@Immutable
data class AppColors(
    val isDark: Boolean,
    val primary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val card: Color,
    val sidebar: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val inputBg: Color,
    val statusConforme: Color,
    val statusNaoConforme: Color,
    val statusNaoAplicavel: Color,
    val statusWarning: Color,
    val cyanAccent: Color
)

val LightAppColors = AppColors(
    isDark = false,
    primary = PrimaryBlue,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    card = LightCard,
    sidebar = LightSidebar,
    border = LightBorder,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted = LightTextMuted,
    inputBg = LightInputBg,
    statusConforme = StatusConforme,
    statusNaoConforme = StatusNaoConforme,
    statusNaoAplicavel = StatusNaoAplicavel,
    statusWarning = StatusWarning,
    cyanAccent = CyanAccent
)

val DarkAppColors = AppColors(
    isDark = true,
    primary = PrimaryBlueDark,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    card = DarkCard,
    sidebar = DarkSidebar,
    border = DarkBorder,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    inputBg = DarkInputBg,
    statusConforme = StatusConformeDark,
    statusNaoConforme = StatusNaoConformeDark,
    statusNaoAplicavel = StatusNaoAplicavelDark,
    statusWarning = StatusWarningDark,
    cyanAccent = CyanAccent
)

// compositionLocalOf ensures only reading composables recompose, rather than invalidating the entire hierarchy
val LocalAppColors = compositionLocalOf { LightAppColors }
val LocalThemeIsDark = compositionLocalOf { false }
val LocalThemeMode = compositionLocalOf { "SYSTEM" }

object AppTheme {
    val colors: AppColors
        @Composable
        get() = LocalAppColors.current

    val isDark: Boolean
        @Composable
        get() = LocalThemeIsDark.current

    val currentMode: String
        @Composable
        get() = LocalThemeMode.current
}
