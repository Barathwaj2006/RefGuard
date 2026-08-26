package com.refguard.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

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
        typography = AppTypography,
        content = content
    )
}
