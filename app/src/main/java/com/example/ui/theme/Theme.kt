package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = VelorixAccent,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1A0A0F),
    onPrimaryContainer = VelorixTextPrimary,
    secondary = VelorixAccentViolet,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF14141A),
    onSecondaryContainer = VelorixTextPrimary,
    tertiary = VelorixEmerald,
    onTertiary = Color(0xFF000000),
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF0C0C10),
    onBackground = VelorixTextPrimary,
    onSurface = VelorixTextPrimary,
    onSurfaceVariant = VelorixTextSecondary,
    error = VelorixRose,
    outline = VelorixBorderSubtle,
    outlineVariant = VelorixBorderLight
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF0033),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFE11D48),
    tertiary = Color(0xFF059669),
    background = Color(0xFFF4F6F9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE9EEF5),
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFE11D48),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFF94A3B8)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = ThemeManager.isDarkTheme.collectAsState().value,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}
