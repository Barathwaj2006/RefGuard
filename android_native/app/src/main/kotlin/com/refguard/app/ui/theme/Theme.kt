package com.refguard.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Palette ─────────────────────────────────────
// Brand: deep navy + electric indigo
val ColorBrand         = Color(0xFF1A237E)    // deep navy
val ColorBrandLight    = Color(0xFF3949AB)    // medium indigo
val ColorBrandSurface  = Color(0xFFF5F5FF)   // near-white tint

// Severity
val ColorSafe          = Color(0xFF2E7D32)    // forest green
val ColorSafeContainer = Color(0xFFE8F5E9)
val ColorWarning       = Color(0xFFF57F17)    // amber
val ColorWarningContainer = Color(0xFFFFF8E1)
val ColorHigh          = Color(0xFFD84315)    // deep orange
val ColorHighContainer = Color(0xFFFBE9E7)
val ColorCritical      = Color(0xFFB71C1C)    // dark red
val ColorCriticalContainer = Color(0xFFFFEBEE)

private val LightColors = lightColorScheme(
    primary = ColorBrand,
    onPrimary = Color.White,
    primaryContainer = ColorBrandSurface,
    onPrimaryContainer = ColorBrand,
    secondary = ColorBrandLight,
    onSecondary = Color.White,
    background = Color(0xFFFAFAFF),
    onBackground = Color(0xFF1C1C2E),
    surface = Color.White,
    onSurface = Color(0xFF1C1C2E),
    surfaceVariant = Color(0xFFF0F0FF),
    onSurfaceVariant = Color(0xFF4A4A6A),
    error = ColorCritical,
    onError = Color.White,
    outline = Color(0xFFB0B0CC)
)

@Composable
fun RefGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
