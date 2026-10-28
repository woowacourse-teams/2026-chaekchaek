package com.chaekchaek.app.ui.archive

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal const val SpineAvailabilitySampleWidth = 42
internal const val SpineAvailabilitySampleHeight = 60

private const val Yes24PreparingImageWidth = 420f
private const val Yes24PreparingImageHeight = 600f

/** YES24가 성공 응답으로 돌려주는 `이미지 준비중` 비트맵을 실제 책등과 구분한다. */
internal fun isKnownImagePreparingSpine(
    sourceWidth: Float,
    sourceHeight: Float,
    pixels: List<Color>,
    sampleWidth: Int = SpineAvailabilitySampleWidth,
    sampleHeight: Int = SpineAvailabilitySampleHeight,
): Boolean {
    if (!sourceWidth.isFinite() || !sourceHeight.isFinite() ||
        abs(sourceWidth - Yes24PreparingImageWidth) > 1f ||
        abs(sourceHeight - Yes24PreparingImageHeight) > 1f ||
        sampleWidth <= 0 || sampleHeight <= 0 ||
        sampleWidth.toLong() * sampleHeight != pixels.size.toLong()
    ) return false

    var lightNeutralPixels = 0
    var centeredGrayInkPixels = 0
    pixels.forEachIndexed { index, color ->
        val channelSpread = max(color.red, max(color.green, color.blue)) -
            min(color.red, min(color.green, color.blue))
        val luminance = color.luminance()
        if (color.alpha >= .9f && channelSpread <= .05f && luminance >= .85f) {
            lightNeutralPixels++
        }

        val x = index % sampleWidth
        val y = index / sampleWidth
        val inCenteredLabel = x in (sampleWidth * 25 / 100)..(sampleWidth * 75 / 100) &&
            y in (sampleHeight * 38 / 100)..(sampleHeight * 62 / 100)
        if (inCenteredLabel && color.alpha >= .9f && channelSpread <= .05f && luminance in .5f..0.85f) {
            centeredGrayInkPixels++
        }
    }

    val mostlyWhiteAndGray = lightNeutralPixels >= pixels.size * 9 / 10
    val hasPreparingLabel = centeredGrayInkPixels >= pixels.size / 100
    return mostlyWhiteAndGray && hasPreparingLabel
}
