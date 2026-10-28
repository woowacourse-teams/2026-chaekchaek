package com.chaekchaek.app.ui.archive

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.chaekchaek.app.ui.theme.LightChaekColors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal val NeutralSpineColor = Color(0xFFAEA999)
internal val NeutralSpinePalette = SpinePalette(NeutralSpineColor, NeutralSpineColor, NeutralSpineColor)

/** 표지 왼쪽의 세 위치를 보존하며, 제목이나 특정 책의 색을 가정하지 않는다. */
internal data class SpinePalette(val top: Color, val middle: Color, val bottom: Color) {
    val stops: Array<Pair<Float, Color>>
        get() = arrayOf(0f to top, .08f to top, .38f to middle, .90f to bottom, 1f to bottom)

    fun backgroundAt(position: Float): Color {
        val y = position.coerceIn(0f, 1f)
        return when {
            y <= .08f -> top
            y <= .38f -> interpolate(top, middle, (y - .08f) / .30f)
            y < .90f -> interpolate(middle, bottom, (y - .38f) / .52f)
            else -> bottom
        }
    }
}

private fun interpolate(from: Color, to: Color, fraction: Float) = Color(
    from.red + (to.red - from.red) * fraction,
    from.green + (to.green - from.green) * fraction,
    from.blue + (to.blue - from.blue) * fraction,
)

/** 각 픽셀 중심이 x 10-25%, y 8/38/90% ±2.5%인 영역의 RGB 채널 중앙값이다.
 * 반투명(alpha < 0.5) 픽셀은 제외하고, 유효 샘플이 없는 영역은 중립색을 사용한다.
 * 작은 이미지에서 영역에 픽셀 중심이 없으면 가장 가까운 한 픽셀을 사용한다.
 */
internal fun coverSpinePalette(pixels: List<Color>, width: Int, height: Int): SpinePalette {
    if (width <= 0 || height <= 0 || width.toLong() * height != pixels.size.toLong()) return NeutralSpinePalette
    val columns = sampleIndices(width, .1f, .25f)
    fun sample(center: Float): Color {
        val rows = sampleIndices(height, center - .025f, center + .025f)
        val colors = rows.flatMap { y -> columns.map { x -> pixels[y * width + x] } }.filter { it.alpha >= .5f }
        if (colors.isEmpty()) return NeutralSpineColor
        return Color(median(colors.map { it.red }), median(colors.map { it.green }), median(colors.map { it.blue }))
    }
    return SpinePalette(sample(.08f), sample(.38f), sample(.90f))
}

private fun sampleIndices(size: Int, start: Float, end: Float): List<Int> =
    (0 until size).filter { (it + .5f) / size in start..end }.ifEmpty {
        listOf((0 until size).minBy { abs((it + .5f) / size - (start + end) / 2f) })
    }

private fun median(values: List<Float>): Float {
    val sorted = values.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 0) (sorted[middle - 1] + sorted[middle]) / 2 else sorted[middle]
}

internal fun spineGlyphInk(background: Color): Color {
    val ink = LightChaekColors.ink
    val luminance = background.luminance()
    val whiteContrast = 1.05f / (luminance + .05f)
    val inkLuminance = ink.luminance()
    val inkContrast = (max(luminance, inkLuminance) + .05f) / (min(luminance, inkLuminance) + .05f)
    return if (whiteContrast > inkContrast) Color.White else ink
}

/** 제목은 축소 없이 한 열로 표시하며 결합 문자와 이모지 묶음을 나누지 않는다. */
internal fun spineTitleGlyphs(title: String, capacity: Int): List<String> {
    if (capacity <= 0) return emptyList()
    val glyphs = spineTitleGraphemes(title).filterNot { glyph -> glyph.all(Char::isWhitespace) }
    return if (glyphs.size > capacity) glyphs.take(capacity - 1) + "…" else glyphs
}

/** 플랫폼의 문자 경계를 사용해 UTF-16 코드 단위가 아닌 표시 문자로 나눈다. */
internal expect fun spineTitleGraphemes(title: String): List<String>
