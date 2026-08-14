package com.chaekchaek.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
  darkColorScheme(
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

private val LightColorScheme =
  lightColorScheme(
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

private val ChaekchaekShapes =
  Shapes(
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
    typography = Typography,
    shapes = ChaekchaekShapes,
    content = content,
  )
}
