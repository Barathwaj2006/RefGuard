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
// Brand: deep navy + electric cyber indigo + vibrant accents
val ColorBrand         = Color(0xFF0D1B48)    // deep cyber navy
val ColorBrandLight    = Color(0xFF2541B2)    // vibrant royal indigo
val ColorBrandAccent   = Color(0xFF00E5FF)    // electric cyber cyan accent
val ColorBrandSurface  = Color(0xFFF3F5FF)    // modern cool surface tint
val ColorBrandDark     = Color(0xFF070D24)    // dark defense hero bg

// Severity & Verification Tokens
val ColorSafe          = Color(0xFF00875A)    // emerald green
val ColorSafeContainer = Color(0xFFE3FCEF)
val ColorWarning       = Color(0xFFFF8B00)    // bright amber
val ColorWarningContainer = Color(0xFFFFF0B3)
val ColorHigh          = Color(0xFFDE350B)    // alert orange
val ColorHighContainer = Color(0xFFFFEBE6)
val ColorCritical      = Color(0xFFBF2600)    // critical crimson
val ColorCriticalContainer = Color(0xFFFFECE6)
val ColorNeutralCard   = Color(0xFFFFFFFF)
val ColorNeutralBorder = Color(0xFFE2E8F0)

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

