package com.example.ui.theme

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeManager {
    private const val PREFS_NAME = "VelorixThemePrefs"
    private const val KEY_IS_DARK = "is_dark_theme"

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isDarkTheme.value = prefs.getBoolean(KEY_IS_DARK, true)
    }

    fun toggleTheme(context: Context? = null) {
        setDarkTheme(!_isDarkTheme.value, context)
    }

    fun setDarkTheme(isDark: Boolean, context: Context? = null) {
        _isDarkTheme.value = isDark
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean(KEY_IS_DARK, isDark)
            ?.apply()
    }
}

val LocalIsDarkTheme = compositionLocalOf { true }

/**
 * Xiaomi HyperOS + Vercel dynamic animated color palette.
 * Automatically animates transitions when switching between Light and Dark themes.
 */
class VelorixDynamicColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val cardBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val borderSubtle: Color,
    val borderLight: Color,
    val accent: Color,
    val accentViolet: Color,
    val inputBackground: Color,
    val dialogBackground: Color,
    val chipBackground: Color,
    val bottomNavBackground: Color
)

@Composable
fun rememberAnimatedThemeColors(isDark: Boolean = LocalIsDarkTheme.current): VelorixDynamicColors {
    val animSpec = tween<Color>(durationMillis = 350, easing = FastOutSlowInEasing)

    val background by animateColorAsState(
        targetValue = if (isDark) Color(0xFF000000) else Color(0xFFF8FAFC),
        animationSpec = animSpec, label = "bg"
    )
    val surface by animateColorAsState(
        targetValue = if (isDark) Color(0xFF0C0C0E) else Color(0xFFFFFFFF),
        animationSpec = animSpec, label = "surface"
    )
    val surfaceElevated by animateColorAsState(
        targetValue = if (isDark) Color(0xFF141416) else Color(0xFFF1F5F9),
        animationSpec = animSpec, label = "surfaceElevated"
    )
    val cardBackground by animateColorAsState(
        targetValue = if (isDark) Color(0xFF0D0D10) else Color(0xFFFFFFFF),
        animationSpec = animSpec, label = "cardBg"
    )
    val textPrimary by animateColorAsState(
        targetValue = if (isDark) Color(0xFFEDEDED) else Color(0xFF0F172A),
        animationSpec = animSpec, label = "textPrimary"
    )
    val textSecondary by animateColorAsState(
        targetValue = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569),
        animationSpec = animSpec, label = "textSecondary"
    )
    val textMuted by animateColorAsState(
        targetValue = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8),
        animationSpec = animSpec, label = "textMuted"
    )
    val borderSubtle by animateColorAsState(
        targetValue = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0),
        animationSpec = animSpec, label = "borderSubtle"
    )
    val borderLight by animateColorAsState(
        targetValue = if (isDark) Color(0xFF3F3F46) else Color(0xFFCBD5E1),
        animationSpec = animSpec, label = "borderLight"
    )
    val accent by animateColorAsState(
        targetValue = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A),
        animationSpec = animSpec, label = "accent"
    )
    val accentViolet by animateColorAsState(
        targetValue = if (isDark) Color(0xFF8B5CF6) else Color(0xFF7C3AED),
        animationSpec = animSpec, label = "accentViolet"
    )
    val inputBackground by animateColorAsState(
        targetValue = if (isDark) Color(0xFF09090B) else Color(0xFFF1F5F9),
        animationSpec = animSpec, label = "inputBg"
    )
    val dialogBackground by animateColorAsState(
        targetValue = if (isDark) Color(0xFF141416) else Color(0xFFFFFFFF),
        animationSpec = animSpec, label = "dialogBg"
    )
    val chipBackground by animateColorAsState(
        targetValue = if (isDark) Color(0xFF18181B) else Color(0xFFE2E8F0),
        animationSpec = animSpec, label = "chipBg"
    )
    val bottomNavBackground by animateColorAsState(
        targetValue = if (isDark) Color(0xE609090B) else Color(0xE6FFFFFF),
        animationSpec = animSpec, label = "navBg"
    )

    return VelorixDynamicColors(
        isDark = isDark,
        background = background,
        surface = surface,
        surfaceElevated = surfaceElevated,
        cardBackground = cardBackground,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        textMuted = textMuted,
        borderSubtle = borderSubtle,
        borderLight = borderLight,
        accent = accent,
        accentViolet = accentViolet,
        inputBackground = inputBackground,
        dialogBackground = dialogBackground,
        chipBackground = chipBackground,
        bottomNavBackground = bottomNavBackground
    )
}
