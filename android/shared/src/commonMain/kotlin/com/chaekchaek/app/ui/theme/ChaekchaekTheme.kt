package com.chaekchaek.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ChaekBackground = Color(0xFFFCFAF7)
val ChaekSurface = Color(0xFFFFFFFF)
val ChaekSurfaceMuted = Color(0xFFF7F2EC)
val ChaekBand = Color(0xFFF1E9DE)
val ChaekInk = Color(0xFF1A1A1A)
val ChaekInkSecondary = Color(0xFF666666)
val ChaekInkTertiary = Color(0xFF999999)
val ChaekBorder = Color(0xFFC9C9C9)
val ChaekBorderSoft = Color(0xFFEEEEEE)
val ChaekAccent = Color(0xFFFF9800)
val ChaekAccentSoft = Color(0xFFFFF4DF)
val ChaekAccentInk = Color(0xFFA05A27)
val ChaekOnDarkMuted = Color(0xB8FFFFFF)
val ChaekDanger = Color(0xFFD94A2B)

private val ChaekDarkBackground = Color(0xFF1A1A1A)
private val ChaekDarkSurface = Color(0xFF242424)
private val ChaekDarkSurfaceMuted = Color(0xFF302C27)
private val ChaekDarkInk = Color(0xFFFCFAF7)
private val ChaekDarkInkSecondary = Color(0xFFC9C3BA)
private val ChaekDarkBorder = Color(0xFF4A4743)
private val ChaekDarkAccent = Color(0xFFFFB74D)

@Immutable
data class TextStyleScale(
    val small: TextStyle,
    val medium: TextStyle,
    val large: TextStyle,
)

object ChaekTextStyles {
    val title = TextStyleScale(
        small = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 22.sp),
        medium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
        large = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp),
    )
    val body = TextStyleScale(
        small = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, lineHeight = 16.sp),
        medium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, lineHeight = 19.sp),
        large = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 22.sp),
    )
    val meta = TextStyleScale(
        small = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, lineHeight = 13.sp, letterSpacing = 0.8.sp),
        medium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.8.sp),
        large = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    )
}

private val ChaekTypography = Typography(
    headlineLarge = ChaekTextStyles.title.large,
    headlineSmall = ChaekTextStyles.title.medium.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = ChaekTextStyles.title.medium,
    titleMedium = ChaekTextStyles.title.small,
    titleSmall = ChaekTextStyles.body.medium.copy(fontWeight = FontWeight.Bold),
    bodyLarge = ChaekTextStyles.body.large,
    bodyMedium = ChaekTextStyles.body.medium,
    bodySmall = ChaekTextStyles.body.small,
    labelLarge = ChaekTextStyles.body.medium.copy(fontWeight = FontWeight.Bold),
    labelMedium = ChaekTextStyles.meta.large,
    labelSmall = ChaekTextStyles.meta.medium,
)

private val LightColorScheme = lightColorScheme(
    primary = ChaekInk,
    onPrimary = ChaekSurface,
    primaryContainer = ChaekAccent,
    onPrimaryContainer = ChaekInk,
    secondary = ChaekAccent,
    onSecondary = ChaekInk,
    secondaryContainer = ChaekAccentSoft,
    onSecondaryContainer = ChaekInk,
    background = ChaekBackground,
    onBackground = ChaekInk,
    surface = ChaekSurface,
    onSurface = ChaekInk,
    surfaceVariant = ChaekSurfaceMuted,
    onSurfaceVariant = ChaekInkSecondary,
    outline = ChaekBorder,
    outlineVariant = ChaekBorderSoft,
    error = ChaekDanger,
)

private val DarkColorScheme = darkColorScheme(
    primary = ChaekDarkAccent,
    onPrimary = ChaekDarkBackground,
    primaryContainer = ChaekDarkAccent,
    onPrimaryContainer = ChaekDarkBackground,
    secondary = ChaekDarkAccent,
    onSecondary = ChaekDarkBackground,
    secondaryContainer = ChaekDarkSurfaceMuted,
    onSecondaryContainer = ChaekDarkInk,
    background = ChaekDarkBackground,
    onBackground = ChaekDarkInk,
    surface = ChaekDarkSurface,
    onSurface = ChaekDarkInk,
    surfaceVariant = ChaekDarkSurfaceMuted,
    onSurfaceVariant = ChaekDarkInkSecondary,
    outline = ChaekDarkBorder,
    outlineVariant = ChaekDarkSurfaceMuted,
    error = ChaekDanger,
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
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = ChaekTypography,
        shapes = ChaekShapes,
        content = content,
    )
}
