package com.chaekchaek.app.ui.theme

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
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.gowun_dodum_regular
import chaekchaek.shared.generated.resources.ibm_plex_sans_kr_regular
import chaekchaek.shared.generated.resources.ibm_plex_sans_kr_semibold
import chaekchaek.shared.generated.resources.nanum_gothic_bold
import chaekchaek.shared.generated.resources.nanum_gothic_regular
import chaekchaek.shared.generated.resources.pretendard_regular
import chaekchaek.shared.generated.resources.pretendard_semibold
import org.jetbrains.compose.resources.Font

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
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF3F3F5),
    band = Color(0xFFF3F3F5),
    ink = Color(0xFF191919),
    inkSecondary = Color(0xFF666666),
    inkTertiary = Color(0xFF858585),
    border = Color(0xFFE0E0E3),
    borderSoft = Color(0xFFE7E7E9),
    accent = Color(0xFFFF8500),
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

enum class ChaekFontCandidate {
    Pretendard,
    IbmPlexSansKr,
    GowunDodum,
    NanumGothic,
}

@Composable
fun ChaekIconFontFamily(): FontFamily = FontFamily(
    Font(Res.font.pretendard_regular, FontWeight.Normal),
    Font(Res.font.pretendard_semibold, FontWeight.SemiBold),
)

@Composable
private fun ChaekFontCandidate.fontFamily(): FontFamily = when (this) {
    ChaekFontCandidate.Pretendard -> ChaekIconFontFamily()
    ChaekFontCandidate.IbmPlexSansKr -> FontFamily(
        Font(Res.font.ibm_plex_sans_kr_regular, FontWeight.Normal),
        Font(Res.font.ibm_plex_sans_kr_semibold, FontWeight.SemiBold),
    )
    ChaekFontCandidate.GowunDodum -> FontFamily(Font(Res.font.gowun_dodum_regular, FontWeight.Normal))
    ChaekFontCandidate.NanumGothic -> FontFamily(
        Font(Res.font.nanum_gothic_regular, FontWeight.Normal),
        Font(Res.font.nanum_gothic_bold, FontWeight.Bold),
    )
}

private fun chaekTypography(fontFamily: FontFamily): Typography {
    val largeTitle = TextStyle(fontFamily = fontFamily, fontSize = 34.sp, lineHeight = 41.sp)
    val title1 = TextStyle(fontFamily = fontFamily, fontSize = 28.sp, lineHeight = 34.sp)
    val title2 = TextStyle(fontFamily = fontFamily, fontSize = 22.sp, lineHeight = 28.sp)
    val title3 = TextStyle(fontFamily = fontFamily, fontSize = 20.sp, lineHeight = 25.sp)
    val headline = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp)
    val body = TextStyle(fontFamily = fontFamily, fontSize = 17.sp, lineHeight = 22.sp)
    val callout = TextStyle(fontFamily = fontFamily, fontSize = 16.sp, lineHeight = 21.sp)
    val footnote = TextStyle(fontFamily = fontFamily, fontSize = 13.sp, lineHeight = 18.sp)
    val caption2 = TextStyle(fontFamily = fontFamily, fontSize = 11.sp, lineHeight = 13.sp)
    return Typography(
        displayLarge = largeTitle,
        displayMedium = title1,
        displaySmall = title2,
        headlineLarge = largeTitle,
        headlineMedium = title1,
        headlineSmall = title2,
        titleLarge = title2,
        titleMedium = title3,
        titleSmall = headline,
        bodyLarge = body,
        bodyMedium = callout,
        bodySmall = footnote,
        labelLarge = headline,
        labelMedium = footnote,
        labelSmall = caption2,
    )
}

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
    darkTheme: Boolean = false,
    fontCandidate: ChaekFontCandidate = ChaekFontCandidate.NanumGothic,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkChaekColors else LightChaekColors
    val typography = chaekTypography(fontCandidate.fontFamily())
    CompositionLocalProvider(LocalChaekColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = typography,
            shapes = ChaekShapes,
            content = content,
        )
    }
}
