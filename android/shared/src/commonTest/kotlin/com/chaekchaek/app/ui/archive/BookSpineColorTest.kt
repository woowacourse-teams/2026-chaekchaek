package com.chaekchaek.app.ui.archive

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class BookSpineColorTest {
    @Test
    fun `긴 제목은 세로 두 줄까지 보존하고 나머지만 말줄임한다`() {
        assertEquals(listOf(listOf('가', '나', '다'), listOf('라', '마', '…')),
            spineTitleColumns("가나 다라마 바사", 3))
        assertEquals(listOf(listOf('채', '식', '주', '의', '자')), spineTitleColumns("채식주의자", 8))
    }

    @Test
    fun `흰 종이 바탕보다 표지의 주요 유채색을 선택한다`() {
        val plum = Color(0xFF572943)
        val pixels = List(80) { Color.White } + List(15) { plum } + List(5) { Color.Blue }
        assertEquals(plum, dominantCoverColor(pixels))
    }

    @Test
    fun `무채색 표지는 실제 평균 명도를 보존한다`() {
        assertEquals(Color.Gray, dominantCoverColor(List(10) { Color.Gray }))
    }

    @Test
    fun `표지 픽셀이 없거나 투명하면 중립색을 사용한다`() {
        assertEquals(NeutralSpineColor, dominantCoverColor(emptyList()))
        assertEquals(NeutralSpineColor, dominantCoverColor(listOf(Color.Transparent)))
    }
}
