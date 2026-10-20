package com.chaekchaek.app.ui.bookdetail

import com.chaekchaek.app.data.remote.BookReview
import com.chaekchaek.app.data.remote.ReviewCreateRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class BookDetailReviewDraftTest {
    @Test
    fun `미선택과 실제 영점 초안을 구분한다`() {
        val unselected = BookDetailReviewDraft(content = "감상")
        val zero = unselected.copy(rating = 0f)

        assertNull(unselected.rating)
        assertEquals(0f, zero.rating)
        assertNotEquals(unselected, zero)
    }

    @Test
    fun `영점부터 오점까지 반점 단위 별점을 담는다`() {
        (0..10).forEach { halfStars ->
            val score = halfStars / 2f
            assertEquals(score, BookDetailReviewDraft(content = "감상", rating = score).rating)
        }
    }

    @Test
    fun `범위 밖이나 반점 단위가 아닌 별점은 거부한다`() {
        listOf(-0.5f, 5.5f, 0.1f, 3.7f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
            .forEach { score ->
                assertFailsWith<IllegalArgumentException> { BookDetailReviewDraft(content = "감상", rating = score) }
            }
    }

    @Test
    fun `신규 감상은 숨긴 읽기 메타데이터를 만들지 않는다`() {
        val request = buildReviewRequest(
            BookDetailReviewDraft(content = "  새 감상  ", quote = " 기억할 문장 ", isSpoiler = true),
        )

        assertEquals(
            ReviewCreateRequest(content = "새 감상", quote = "기억할 문장", isSpoiler = true),
            request,
        )
        assertNull(request.chapter)
        assertNull(request.currentPage)
        assertNull(request.totalPages)
    }

    @Test
    fun `감상 수정은 화면에서 숨긴 기존 챕터와 쪽수를 보존한다`() {
        val original = review(chapter = "3장", currentPage = 120)
        val request = buildReviewRequest(
            BookDetailReviewDraft(content = "수정한 감상", quote = "  ", isSpoiler = true),
            original,
        )

        assertEquals("수정한 감상", request.content)
        assertNull(request.quote)
        assertEquals("3장", request.chapter)
        assertEquals(120, request.currentPage)
        assertEquals(true, request.isSpoiler)
        assertNull(request.totalPages)
    }

    @Test
    fun `기존 영쪽과 미입력 읽기 메타데이터를 그대로 보존한다`() {
        val draft = BookDetailReviewDraft(content = "수정한 감상")

        assertEquals(0, buildReviewRequest(draft, review(chapter = null, currentPage = 0)).currentPage)
        val noMetadata = buildReviewRequest(draft, review(chapter = null, currentPage = null))
        assertNull(noMetadata.chapter)
        assertNull(noMetadata.currentPage)
    }

    @Test
    fun `별점이 있는 시안 초안을 기존 서버 요청으로 변환하지 않는다`() {
        listOf(0f, 3.5f, 5f).forEach { rating ->
            assertFailsWith<IllegalArgumentException> {
                buildReviewRequest(BookDetailReviewDraft(content = "감상", rating = rating))
            }
        }
    }

    private fun review(chapter: String?, currentPage: Int?) = BookReview(
        reviewId = 1,
        content = "기존 감상",
        quote = "기존 문구",
        chapter = chapter,
        currentPage = currentPage,
        createdAt = "2026-10-21T00:00:00Z",
        authorName = "참새",
        anonymous = true,
        replyCount = 0,
        likeCount = 0,
    )
}
