package com.chaekchaek.app.ui.bookdetail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BookQuestionContentTest {
    @Test
    fun `질문만 있으면 참고 자료 탭을 선택하지 않는다`() {
        val question = BookQuestionContent(id = "question-1", markdown = "**무엇이 남았나요?**")

        assertEquals(emptyList(), question.availableTabs)
        assertNull(question.selectedReferenceTab())
        assertNull(question.referenceMarkdown(BookQuestionReferenceTab.Plot))
        assertNull(question.referenceMarkdown(BookQuestionReferenceTab.Excerpt))
    }

    @Test
    fun `공백 자료는 제외하고 실제 있는 자료로 선택을 복원한다`() {
        val question = BookQuestionContent(
            id = "question-1",
            markdown = "어떤 장면이 남았나요?",
            plotMarkdown = " \n\t",
            excerptMarkdown = "> 기억할 장면",
        )

        assertEquals(listOf(BookQuestionReferenceTab.Excerpt), question.availableTabs)
        assertEquals(BookQuestionReferenceTab.Excerpt, question.selectedReferenceTab(BookQuestionReferenceTab.Plot))
        assertEquals("> 기억할 장면", question.referenceMarkdown(BookQuestionReferenceTab.Excerpt))
    }

    @Test
    fun `두 자료가 있으면 줄거리를 먼저 열고 유효한 탭 선택을 유지한다`() {
        val question = BookQuestionContent(
            id = "question-1",
            markdown = "왜 그렇게 생각하나요?",
            plotMarkdown = "**줄거리**",
            excerptMarkdown = "*발췌*",
        )

        assertEquals(listOf(BookQuestionReferenceTab.Plot, BookQuestionReferenceTab.Excerpt), question.availableTabs)
        assertEquals(BookQuestionReferenceTab.Plot, question.selectedReferenceTab())
        assertEquals(BookQuestionReferenceTab.Excerpt, question.selectedReferenceTab(BookQuestionReferenceTab.Excerpt))
        assertEquals("**줄거리**", question.referenceMarkdown(BookQuestionReferenceTab.Plot))
    }

    @Test
    fun `자료가 사라지면 이전 탭 선택을 남기지 않는다`() {
        val question = BookQuestionContent(id = "question-1", markdown = "무엇이 남았나요?")

        assertNull(question.selectedReferenceTab(BookQuestionReferenceTab.Excerpt))
    }

    @Test
    fun `내용이나 식별자가 없는 질문은 만들지 않는다`() {
        assertFailsWith<IllegalArgumentException> { BookQuestionContent(id = " ", markdown = "질문") }
        assertFailsWith<IllegalArgumentException> { BookQuestionContent(id = "question-1", markdown = "\n ") }
    }

    @Test
    fun `오버레이는 가용 높이 비율과 최소 최대 높이를 따른다`() {
        assertEquals(340f, bookQuestionOverlayHeight(1000f))
        assertEquals(307.2f, bookQuestionOverlayHeight(640f), absoluteTolerance = 0.001f)
        assertEquals(280f, bookQuestionOverlayHeight(500f))
    }

    @Test
    fun `작은 화면에서는 가용 공간보다 커지지 않는다`() {
        assertEquals(280f, bookQuestionOverlayHeight(280f))
        assertEquals(220f, bookQuestionOverlayHeight(220f))
        assertEquals(0f, bookQuestionOverlayHeight(0f))
    }

    @Test
    fun `유효하지 않은 가용 높이는 거부한다`() {
        listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach { height ->
            assertFailsWith<IllegalArgumentException> { bookQuestionOverlayHeight(height) }
        }
    }
}
