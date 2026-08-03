package com.chamsae.chaekchaek.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ColorRuleDark = androidx.compose.ui.graphics.Color(0xFF55584B)

private val DarkColorScheme =
  darkColorScheme(
    primary = NightOlive,
    onPrimary = OliveDark,
    primaryContainer = OliveDark,
    onPrimaryContainer = NightOlive,
    background = NightPaper,
    onBackground = Paper,
    surface = NightSurface,
    onSurface = Paper,
    surfaceVariant = OliveDark,
    onSurfaceVariant = OlivePale,
    outline = ColorRuleDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Olive,
    onPrimary = PaperSurface,
    primaryContainer = OlivePale,
    onPrimaryContainer = OliveDark,
    secondary = OliveDark,
    onSecondary = PaperSurface,
    background = Paper,
    onBackground = Ink,
    surface = PaperSurface,
    onSurface = Ink,
    surfaceVariant = PaperMuted,
    onSurfaceVariant = InkMuted,
    outline = Rule,
  )

@Composable
fun ChaekchaekTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme, typography = Typography, content = content)
}
