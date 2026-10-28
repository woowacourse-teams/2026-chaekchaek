package com.chaekchaek.app.ui.archive

import androidx.compose.ui.graphics.Color
import com.chaekchaek.app.ui.theme.LightChaekColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookSpineColorTest {
    @Test
    fun `한 열 제목은 공백을 빼고 마지막 칸에서 말줄임한다`() {
        assertEquals(listOf("가", "나", "…"), spineTitleGlyphs("가나 다라마 바사", 3))
        assertEquals(listOf("채", "식", "주", "의", "자"), spineTitleGlyphs("채식주의자", 8))
        assertEquals(listOf("…"), spineTitleGlyphs("긴 제목", 1))
        assertEquals(emptyList(), spineTitleGlyphs("작은 썸네일", 0))
        assertEquals(emptyList(), spineTitleGlyphs("  ", 8))
        assertEquals(listOf("📚", "책"), spineTitleGlyphs("📚 책", 2))
    }

    @Test
    fun `결합 악센트와 분해 한글은 각각 한 칸을 차지한다`() {
        assertEquals(listOf("C", "a", "f", "e\u0301"), spineTitleGlyphs("Cafe\u0301", 4))
        assertEquals(listOf("\u1112\u1161\u11ab", "글"), spineTitleGlyphs("\u1112\u1161\u11ab글", 2))
    }

    @Test
    fun `연결 이모지와 피부색과 국기는 한 칸에서 보존한다`() {
        val glyphs = listOf("👩‍👩‍👧‍👦", "👍🏽", "🇰🇷", "1️⃣")
        assertEquals(glyphs, spineTitleGlyphs(glyphs.joinToString(" "), 4))
        assertEquals(listOf(glyphs.first(), "…"), spineTitleGlyphs(glyphs.joinToString(""), 2))
    }

    @Test
    fun `표지 왼쪽의 상중하 색은 오른쪽 제목과 작은 잡음에 바뀌지 않는다`() {
        val top = Color(0xFF253C5E)
        val middle = Color(0xFF977F76)
        val bottom = Color(0xFFD6A783)
        val pixels = List(48 * 48) { index ->
            val x = index % 48
            val y = index / 48
            when {
                x >= 12 -> Color.Red
                x == 5 -> Color.Black
                y < 16 -> top
                y < 32 -> middle
                else -> bottom
            }
        }
        assertEquals(SpinePalette(top, middle, bottom), coverSpinePalette(pixels, 48, 48))
    }

    @Test
    fun `각 채널 중앙값은 소수의 표지 글자 픽셀을 무시한다`() {
        val paper = Color(0xFFEDEDEA)
        val pixels = MutableList(48 * 48) { paper }
        pixels[3 * 48 + 5] = Color.Black
        pixels[18 * 48 + 5] = Color.Red
        assertEquals(SpinePalette(paper, paper, paper), coverSpinePalette(pixels, 48, 48))
    }

    @Test
    fun `한 픽셀 표지와 무채색 표지도 원래 색을 보존한다`() {
        for (color in listOf(Color.White, Color.Black, Color.Gray)) {
            assertEquals(SpinePalette(color, color, color), coverSpinePalette(listOf(color), 1, 1))
        }
    }

    @Test
    fun `이미지가 비었거나 샘플이 투명하면 중립 팔레트를 사용한다`() {
        assertEquals(NeutralSpinePalette, coverSpinePalette(emptyList(), 0, 0))
        assertEquals(NeutralSpinePalette, coverSpinePalette(listOf(Color.Red), 48, 48))
        assertEquals(NeutralSpinePalette, coverSpinePalette(List(48 * 48) { Color.Red.copy(alpha = .3f) }, 48, 48))
        assertEquals(SpinePalette(Color.Red, Color.Red, Color.Red), coverSpinePalette(listOf(Color.Red.copy(alpha = .5f)), 1, 1))
    }

    @Test
    fun `YES24 이미지 준비중 비트맵은 실제 책등이 아니다`() {
        val pixels = MutableList(SpineAvailabilitySampleWidth * SpineAvailabilitySampleHeight) { Color.White }
        for (y in 24..35) for (x in 14..27) {
            pixels[y * SpineAvailabilitySampleWidth + x] = Color(0xFFD0D0D0)
        }

        assertTrue(isKnownImagePreparingSpine(420f, 600f, pixels))
        assertFalse(isKnownImagePreparingSpine(76f, 1200f, pixels))
        assertFalse(isKnownImagePreparingSpine(420f, 600f, List(pixels.size) { Color.White }))
        assertFalse(isKnownImagePreparingSpine(420f, 600f, List(pixels.size) { Color(0xFF80CDB2) }))
    }

    @Test
    fun `그라데이션 색 위치를 보존하고 글자마다 대비가 큰 흰색과 먹색을 고른다`() {
        val palette = SpinePalette(Color(0xFF253C5E), Color(0xFF977F76), Color(0xFFD6A783))
        assertEquals(palette.top, palette.backgroundAt(0f))
        assertEquals(palette.top, palette.backgroundAt(.08f))
        assertEquals(palette.middle, palette.backgroundAt(.38f))
        assertEquals(palette.bottom, palette.backgroundAt(.90f))
        assertEquals(palette.bottom, palette.backgroundAt(1f))
        assertEquals(listOf(Color.White, Color.White, Color.White, LightChaekColors.ink),
            (0..3).map { index -> spineGlyphInk(palette.backgroundAt((14f + 21f * (index + .5f)) / 220f)) })
        assertEquals(Color.White, spineGlyphInk(Color.Black))
        assertEquals(LightChaekColors.ink, spineGlyphInk(Color.White))
        val transition = palette.backgroundAt(.23f)
        assertTrue(transition.red > palette.top.red && transition.red < palette.middle.red)
    }
}
