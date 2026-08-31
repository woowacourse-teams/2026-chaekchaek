package com.chaekchaek.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class ChaekColors(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val band: Color,
    val ink: Color,
    val inkSecondary: Color,
    val inkTertiary: Color,
    val border: Color,
    val borderSoft: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentInk: Color,
    val onDarkMuted: Color,
    val danger: Color,
)

internal val LightChaekColors = ChaekColors(
    background = Color(0xFFFCFAF7),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF7F2EC),
    band = Color(0xFFF1E9DE),
    ink = Color(0xFF1A1A1A),
    inkSecondary = Color(0xFF666666),
    inkTertiary = Color(0xFF999999),
    border = Color(0xFFC9C9C9),
    borderSoft = Color(0xFFEEEEEE),
    accent = Color(0xFFFF9800),
    accentSoft = Color(0xFFFFF4DF),
    accentInk = Color(0xFFA05A27),
    onDarkMuted = Color(0xB8FFFFFF),
    danger = Color(0xFFC92A24),
)

internal val DarkChaekColors = ChaekColors(
    background = Color(0xFF1A1A1A),
    surface = Color(0xFF242424),
    surfaceMuted = Color(0xFF302C27),
    band = Color(0xFF4A4035),
    ink = Color(0xFFFCFAF7),
    inkSecondary = Color(0xFFC9C3BA),
    inkTertiary = Color(0xFFAAA39A),
    border = Color(0xFF7A7570),
    borderSoft = Color(0xFF7A7570),
    accent = Color(0xFFFFB74D),
    accentSoft = Color(0xFF4A3520),
    accentInk = Color(0xFFFFBF66),
    onDarkMuted = Color(0xB8FFFFFF),
    danger = Color(0xFFFF6B5A),
)

internal val LocalChaekColors = staticCompositionLocalOf { LightChaekColors }

val ChaekBackground: Color @Composable get() = LocalChaekColors.current.background
val ChaekSurface: Color @Composable get() = LocalChaekColors.current.surface
val ChaekSurfaceMuted: Color @Composable get() = LocalChaekColors.current.surfaceMuted
val ChaekBand: Color @Composable get() = LocalChaekColors.current.band
val ChaekInk: Color @Composable get() = LocalChaekColors.current.ink
val ChaekInkSecondary: Color @Composable get() = LocalChaekColors.current.inkSecondary
val ChaekInkTertiary: Color @Composable get() = LocalChaekColors.current.inkTertiary
val ChaekBorder: Color @Composable get() = LocalChaekColors.current.border
val ChaekBorderSoft: Color @Composable get() = LocalChaekColors.current.borderSoft
val ChaekAccent: Color @Composable get() = LocalChaekColors.current.accent
val ChaekAccentSoft: Color @Composable get() = LocalChaekColors.current.accentSoft
val ChaekAccentInk: Color @Composable get() = LocalChaekColors.current.accentInk
val ChaekOnDarkMuted: Color @Composable get() = LocalChaekColors.current.onDarkMuted
val ChaekDanger: Color @Composable get() = LocalChaekColors.current.danger

object ChaekTextStyles {
    val largeTitle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 34.sp, lineHeight = 41.sp)
    val title1 = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 28.sp, lineHeight = 34.sp)
    val title2 = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 22.sp, lineHeight = 28.sp)
    val title3 = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 20.sp, lineHeight = 25.sp)
    val headline = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp)
    val body = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 17.sp, lineHeight = 22.sp)
    val callout = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 21.sp)
    val subhead = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 20.sp)
    val footnote = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, lineHeight = 18.sp)
    val caption1 = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 16.sp)
    val caption2 = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, lineHeight = 13.sp)
}

private val ChaekTypography = Typography(
    displayLarge = ChaekTextStyles.largeTitle,
    displayMedium = ChaekTextStyles.title1,
    displaySmall = ChaekTextStyles.title2,
    headlineLarge = ChaekTextStyles.largeTitle,
    headlineMedium = ChaekTextStyles.title1,
    headlineSmall = ChaekTextStyles.title2,
    titleLarge = ChaekTextStyles.title2,
    titleMedium = ChaekTextStyles.title3,
    titleSmall = ChaekTextStyles.headline,
    bodyLarge = ChaekTextStyles.body,
    bodyMedium = ChaekTextStyles.callout,
    bodySmall = ChaekTextStyles.footnote,
    labelLarge = ChaekTextStyles.headline,
    labelMedium = ChaekTextStyles.footnote.copy(fontFamily = FontFamily.Monospace),
    labelSmall = ChaekTextStyles.caption2.copy(fontFamily = FontFamily.Monospace),
)

private val LightColorScheme = lightColorScheme(
    primary = LightChaekColors.ink,
    onPrimary = LightChaekColors.surface,
    primaryContainer = LightChaekColors.accent,
    onPrimaryContainer = LightChaekColors.ink,
    secondary = LightChaekColors.accent,
    onSecondary = LightChaekColors.ink,
    secondaryContainer = LightChaekColors.accentSoft,
    onSecondaryContainer = LightChaekColors.ink,
    background = LightChaekColors.background,
    onBackground = LightChaekColors.ink,
    surface = LightChaekColors.surface,
    onSurface = LightChaekColors.ink,
    surfaceVariant = LightChaekColors.surfaceMuted,
    onSurfaceVariant = LightChaekColors.inkSecondary,
    outline = LightChaekColors.border,
    outlineVariant = LightChaekColors.borderSoft,
    error = LightChaekColors.danger,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkChaekColors.accent,
    onPrimary = DarkChaekColors.background,
    primaryContainer = DarkChaekColors.accent,
    onPrimaryContainer = DarkChaekColors.background,
    secondary = DarkChaekColors.accent,
    onSecondary = DarkChaekColors.background,
    secondaryContainer = DarkChaekColors.accentSoft,
    onSecondaryContainer = DarkChaekColors.ink,
    background = DarkChaekColors.background,
    onBackground = DarkChaekColors.ink,
    surface = DarkChaekColors.surface,
    onSurface = DarkChaekColors.ink,
    surfaceVariant = DarkChaekColors.surfaceMuted,
    onSurfaceVariant = DarkChaekColors.inkSecondary,
    outline = DarkChaekColors.border,
    outlineVariant = DarkChaekColors.borderSoft,
    error = DarkChaekColors.danger,
)

private val ChaekShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ChaekchaekTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkChaekColors else LightChaekColors
    CompositionLocalProvider(LocalChaekColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = ChaekTypography,
            shapes = ChaekShapes,
            content = content,
        )
    }
}
