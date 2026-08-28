package com.refguard.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = ColorBrand,
    onPrimary = Color.White,
    primaryContainer = ColorBrandSurface,
    onPrimaryContainer = Color(0xFF001E36),
    secondary = Color(0xFF0288D1),
    onSecondary = Color.White,
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    onSurface = Color(0xFF1F1F1F)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF81D4FA),
    onSecondary = Color(0xFF00344F),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE3E3E3)
)

@Composable
fun RefGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
