package com.chaekchaek.app.ui.archive

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class BookshelfSpineSizingTest {
    @Test
    fun `책등 높이는 순서와 무관하게 220dp를 사용한다`() {
        assertEquals(220.dp, BookshelfSpineHeight)
        assertEquals(110.dp, scaledBookshelfSpineHeight(0.7f))
    }

    @Test
    fun `책등 두께는 이미지 비율에 따라 달라진다`() {
        assertEquals(22.dp, bookshelfSpineThickness(220.dp, 40f, 400f, 30.dp))
        assertEquals(154.dp, bookshelfSpineThickness(220.dp, 280f, 400f, 30.dp))
    }

    @Test
    fun `이미지 크기를 알 수 없으면 대체 책등 두께를 사용한다`() {
        assertEquals(30.dp, bookshelfSpineThickness(220.dp, Float.NaN, Float.NaN, 30.dp))
    }
}
