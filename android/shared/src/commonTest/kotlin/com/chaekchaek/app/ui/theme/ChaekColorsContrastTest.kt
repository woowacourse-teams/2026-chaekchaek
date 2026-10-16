package com.chaekchaek.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.chaekchaek.app.ui.bookdetail.ArchiveStageBackground
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

class ChaekColorsContrastTest {
    private val backgrounds = mapOf(
        "background" to DarkChaekColors.background,
        "surface" to DarkChaekColors.surface,
        "surfaceMuted" to DarkChaekColors.surfaceMuted,
    )

    @Test
    fun darkTextColorsMeetWcagAa() {
        val textColors = mapOf(
            "ink" to DarkChaekColors.ink,
            "inkSecondary" to DarkChaekColors.inkSecondary,
            "inkTertiary" to DarkChaekColors.inkTertiary,
            "accentInk" to DarkChaekColors.accentInk,
            "onDarkMuted" to DarkChaekColors.onDarkMuted,
            "danger" to DarkChaekColors.danger,
        )

        textColors.forEach { (textName, textColor) ->
            backgrounds.forEach { (backgroundName, backgroundColor) ->
                val ratio = contrastRatio(textColor.compositeOver(backgroundColor), backgroundColor)
                assertTrue(ratio >= 4.5, "$textName on $backgroundName contrast was $ratio")
            }
        }
    }

    @Test
    fun darkBordersAndAccentMeetNonTextContrast() {
        val nonTextColors = mapOf(
            "border" to DarkChaekColors.border,
            "borderSoft" to DarkChaekColors.borderSoft,
            "accent" to DarkChaekColors.accent,
        )

        nonTextColors.forEach { (colorName, color) ->
            backgrounds.forEach { (backgroundName, backgroundColor) ->
                val ratio = contrastRatio(color, backgroundColor)
                assertTrue(ratio >= 3.0, "$colorName on $backgroundName contrast was $ratio")
            }
        }
    }

    @Test
    fun darkScreenTextPairsMeetWcagAa() {
        assertTextContrast("accent on archive stage", DarkChaekColors.accent, ArchiveStageBackground)
        assertTextContrast("inkSecondary on surfaceMuted", DarkChaekColors.inkSecondary, DarkChaekColors.surfaceMuted)
    }

    private fun assertTextContrast(name: String, foreground: Color, background: Color) {
        val ratio = contrastRatio(foreground, background)
        assertTrue(ratio >= 4.5, "$name contrast was $ratio")
    }
}

private fun contrastRatio(foreground: Color, background: Color): Double {
    val foregroundLuminance = foreground.relativeLuminance()
    val backgroundLuminance = background.relativeLuminance()
    return (max(foregroundLuminance, backgroundLuminance) + 0.05) /
        (min(foregroundLuminance, backgroundLuminance) + 0.05)
}

private fun Color.relativeLuminance(): Double =
    0.2126 * red.toDouble().linearized() +
        0.7152 * green.toDouble().linearized() +
        0.0722 * blue.toDouble().linearized()

private fun Double.linearized(): Double =
    if (this <= 0.04045) this / 12.92 else ((this + 0.055) / 1.055).pow(2.4)

private fun Color.compositeOver(background: Color): Color = Color(
    red = red * alpha + background.red * (1f - alpha),
    green = green * alpha + background.green * (1f - alpha),
    blue = blue * alpha + background.blue * (1f - alpha),
    alpha = 1f,
)
