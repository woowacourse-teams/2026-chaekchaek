package com.chaekchaek.app.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class TextStyleScale(
  val small: TextStyle,
  val medium: TextStyle,
  val large: TextStyle,
)

object ChaekTextStyles {
  val title =
    TextStyleScale(
      small = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 22.sp),
      medium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
      large = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp),
    )

  val body =
    TextStyleScale(
      small = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, lineHeight = 16.sp),
      medium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, lineHeight = 19.sp),
      large = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 22.sp),
    )

  val meta =
    TextStyleScale(
      small = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, lineHeight = 13.sp, letterSpacing = 0.8.sp),
      medium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.8.sp),
      large = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    )
}

val Typography =
  Typography(
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
