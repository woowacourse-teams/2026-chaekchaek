package com.chaekchaek.app.ui.bookdetail

import com.chaekchaek.app.data.remote.BookReview
import com.chaekchaek.app.data.remote.ReplyPage
import com.chaekchaek.app.data.remote.ReviewPage
import com.chaekchaek.app.data.remote.ReviewReply
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeletedContentDisplayTest {
    @Test
    fun deletedReviewsRemainVisibleAndIncludedInServerCount() {
        val active = review(1)
        val deleted = review(2).copy(deleted = true, replyCount = 1, recentReplies = listOf(reply(3)))
        val display = reviewDisplay(listOf(deleted, active), totalCount = 2, hasMore = false)

        assertEquals(listOf(2L, 1L), display.reviews.map { it.reviewId })
        assertEquals("삭제된 감상입니다.", display.reviews.first().content)
        assertEquals(deleted.recentReplies, display.reviews.first().recentReplies)
        assertEquals(2, display.count)
        assertFalse(display.showEmpty)
    }

    @Test
    fun deletedReviewRemainsVisibleBeforeRepliesAreLoaded() {
        val deleted = review(1).copy(deleted = true, replyCount = 2)
        for (hasMore in listOf(true, false)) {
            val display = reviewDisplay(listOf(deleted), totalCount = 3, hasMore = hasMore)
            assertEquals(1, display.reviews.size)
            assertEquals(3, display.count)
            assertFalse(display.showEmpty)
        }
    }

    @Test
    fun emptyServerResultShowsZeroCountAfterPaginationFinishes() {
        assertTrue(reviewDisplay(emptyList(), 0, hasMore = false).showEmpty)
        assertFalse(reviewDisplay(emptyList(), 3, hasMore = true).showEmpty)
    }

    @Test
    fun deletedReviewContentIsMaskedWithoutChangingRepliesOrPagination() {
        val deleted = review(2).copy(deleted = true, quote = "인용", chapter = "장", recentReplies = listOf(reply(3)))
        val response = ReviewPage(2, 2, listOf(deleted, review(1)))
        repeat(2) {
            val masked = visibleReviews(response.items).first()
            assertEquals("삭제된 감상입니다.", masked.content)
            assertEquals(null, masked.quote)
            assertEquals(null, masked.chapter)
            assertEquals(deleted.recentReplies, masked.recentReplies)
        }
        assertEquals(2, response.totalCount)
        assertEquals(2, response.nextPage)
        assertEquals("감상", response.items.first().content)
    }

    @Test
    fun deletedReviewDisallowsNewRepliesAndLikesButAllowsUnlike() {
        assertFalse(canReplyToReview(deleted = true))
        assertFalse(canToggleReviewLike(deleted = true, likedByMe = false))
        assertTrue(canToggleReviewLike(deleted = true, likedByMe = true))
        assertTrue(canReplyToReview(deleted = false))
        assertTrue(canToggleReviewLike(deleted = false, likedByMe = false))
    }

    @Test
    fun deletedReplyPreviewCanStillOpenUnloadedReplies() {
        val deleted = reply(1).copy(deleted = true)

        val display = replyDisplay(listOf(deleted), totalCount = 2)

        assertTrue(display.replies.isEmpty())
        assertTrue(display.hasMore)
    }

    @Test
    fun allReplyPagesAreLoadedEvenWhenFirstPageOnlyContainsDeletedReplies() = runTest {
        val deleted = reply(1).copy(deleted = true)
        val active = reply(2)
        val loadedPages = mutableListOf<Int>()
        val allReplies = loadAllReplies { page ->
            loadedPages += page
            if (page == 1) ReplyPage(2, 2, listOf(deleted))
            else ReplyPage(2, null, listOf(active))
        }

        val display = replyDisplay(allReplies, totalCount = 2)

        assertEquals(listOf(1, 2), loadedPages)
        assertEquals(listOf(active), display.replies)
        assertFalse(display.hasMore)
        assertEquals(listOf(deleted, active), allReplies)
    }

    @Test
    fun fullyDeletedRepliesLeaveNoVisibleReplySection() {
        val display = replyDisplay(listOf(reply(1).copy(deleted = true)), totalCount = 1)

        assertTrue(display.replies.isEmpty())
        assertFalse(display.hasMore)
    }

    private fun reply(id: Long) = ReviewReply(id, "답글", "작성자", false, 0, writtenByMe = true)

    private fun review(id: Long) = BookReview(
        reviewId = id,
        content = "감상",
        quote = null,
        chapter = null,
        currentPage = null,
        createdAt = "2026-10-06T06:03:22Z",
        authorName = "작성자",
        anonymous = false,
        replyCount = 0,
        likeCount = 0,
        writtenByMe = true,
    )
}
