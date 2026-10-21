package com.chaekchaek.app.ui.bookdetail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookQuestionViewportTest {
    @Test
    fun `가로 화면에서도 본문과 배경 목록의 독립 스크롤 공간을 남긴다`() {
        val landscape = bookQuestionOverlayLayout(375f, 297f)
        assertEquals(221f, landscape.height)
        assertFalse(landscape.sideScrollGutter)
        val compact = bookQuestionOverlayLayout(320f, 194f)
        assertEquals(170f, compact.height)
        assertTrue(compact.sideScrollGutter)
        assertEquals(340f, bookQuestionOverlayLayout(844f, 766f).height)
    }
}
