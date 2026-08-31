package com.relatopro.app.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightMaterialColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = Color.White,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder
)

private val DarkMaterialColorScheme = darkColorScheme(
    primary = PrimaryBlueDark,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.White,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder
)

@Composable
fun RelatoProTheme(
    themeMode: String = "SYSTEM",
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemDark
    }

    val targetColors = if (isDark) DarkAppColors else LightAppColors
    val animSpec = tween<Color>(durationMillis = 150)

    // Smooth interpolated color transitions across theme toggle (eliminating flashing and white/black glitches)
    val animatedPrimary by animateColorAsState(targetColors.primary, animSpec, label = "primary")
    val animatedBackground by animateColorAsState(targetColors.background, animSpec, label = "background")
    val animatedSurface by animateColorAsState(targetColors.surface, animSpec, label = "surface")
    val animatedSurfaceVariant by animateColorAsState(targetColors.surfaceVariant, animSpec, label = "surfaceVariant")
    val animatedCard by animateColorAsState(targetColors.card, animSpec, label = "card")
    val animatedSidebar by animateColorAsState(targetColors.sidebar, animSpec, label = "sidebar")
    val animatedBorder by animateColorAsState(targetColors.border, animSpec, label = "border")
    val animatedTextPrimary by animateColorAsState(targetColors.textPrimary, animSpec, label = "textPrimary")
    val animatedTextSecondary by animateColorAsState(targetColors.textSecondary, animSpec, label = "textSecondary")
    val animatedTextMuted by animateColorAsState(targetColors.textMuted, animSpec, label = "textMuted")
    val animatedInputBg by animateColorAsState(targetColors.inputBg, animSpec, label = "inputBg")
    val animatedStatusConforme by animateColorAsState(targetColors.statusConforme, animSpec, label = "statusConforme")
    val animatedStatusNaoConforme by animateColorAsState(targetColors.statusNaoConforme, animSpec, label = "statusNaoConforme")
    val animatedStatusNaoAplicavel by animateColorAsState(targetColors.statusNaoAplicavel, animSpec, label = "statusNaoAplicavel")
    val animatedStatusWarning by animateColorAsState(targetColors.statusWarning, animSpec, label = "statusWarning")

    val currentAppColors = remember(isDark, animatedBackground, animatedSurface, animatedTextPrimary) {
        AppColors(
            isDark = isDark,
            primary = animatedPrimary,
            background = animatedBackground,
            surface = animatedSurface,
            surfaceVariant = animatedSurfaceVariant,
            card = animatedCard,
            sidebar = animatedSidebar,
            border = animatedBorder,
            textPrimary = animatedTextPrimary,
            textSecondary = animatedTextSecondary,
            textMuted = animatedTextMuted,
            inputBg = animatedInputBg,
            statusConforme = animatedStatusConforme,
            statusNaoConforme = animatedStatusNaoConforme,
            statusNaoAplicavel = animatedStatusNaoAplicavel,
            statusWarning = animatedStatusWarning,
            cyanAccent = CyanAccent
        )
    }

    val materialColorScheme = if (isDark) DarkMaterialColorScheme else LightMaterialColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = if (isDark) DarkBackground.toArgb() else LightSurface.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalAppColors provides currentAppColors,
        LocalThemeIsDark provides isDark,
        LocalThemeMode provides themeMode
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = RelatoProTypography,
            content = content
        )
    }
}
