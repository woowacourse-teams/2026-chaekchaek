package com.chaekchaek.app.ui.bookdetail

import com.chaekchaek.app.domain.rating.Rating
import com.chaekchaek.app.data.remote.ReplyPage
import com.chaekchaek.app.data.remote.ReviewReply
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BookDetailRulesTest {
    @Test
    fun reviewValidationAndSpoilerMaskKeepAndroidBehavior() {
        assertTrue(BookDetailInputRules.canSubmitReview("좋았다", "80", 308))
        assertFalse(BookDetailInputRules.canSubmitReview("  ", "80", 308))
        assertFalse(BookDetailInputRules.canSubmitReview("좋았다", "309", 308))
        assertNull(BookDetailInputRules.validPage("-1", 308))
        assertFalse(BookDetailInputRules.hasReviewDraft("", "", "", "80", 80, false))
        assertTrue(BookDetailInputRules.hasReviewDraft("초안", "", "", "80", 80, false))
        assertTrue(shouldLockReview(reviewId = 1, isSpoiler = true, revealedReviewIds = emptySet()))
        assertFalse(shouldLockReview(reviewId = 1, isSpoiler = false, revealedReviewIds = emptySet()))
        assertEquals("짹짹 짹짹짹!\n짹짹?", maskAsChirps("감상 42쪽!\n좋다?"))
    }

    @Test
    fun spoilerRevealOnlyUnlocksSelectedReview() {
        val revealedReviewIds = setOf(1L)

        assertFalse(shouldLockReview(reviewId = 1, isSpoiler = true, revealedReviewIds = revealedReviewIds))
        assertTrue(shouldLockReview(reviewId = 2, isSpoiler = true, revealedReviewIds = revealedReviewIds))
        assertFalse(shouldLockReview(reviewId = 3, isSpoiler = false, revealedReviewIds = revealedReviewIds))
        assertFalse(BookDetailInputRules.canSubmitPage(null))
        assertTrue(BookDetailInputRules.canSubmitPage(80))
    }

    @Test
    fun repliesLoadEveryPageOnceAndRemoveDuplicates() = runTest {
        val loadedPages = mutableListOf<Int>()
        val reply = ReviewReply(1, "답글", "참새", false, 0)

        val result = loadAllReplies { page ->
            loadedPages += page
            if (page == 1) ReplyPage(2, 2, listOf(reply)) else ReplyPage(2, null, listOf(reply))
        }

        assertEquals(listOf(1, 2), loadedPages)
        assertEquals(listOf(reply), result)
    }

    @Test
    fun ratingAndReplyRulesKeepBoundaries() {
        assertEquals(Rating.ofHalfStars(1), RatingDialogRules.ratingAtSlot(0))
        assertEquals("4.0 · 좋았어요", RatingDialogRules.label(Rating.ofHalfStars(8)))
        assertTrue(ReplyInputRules.canSubmit("좋아요"))
        assertFalse(ReplyInputRules.canSubmit(" "))
        assertFalse(ReplyInputRules.canSubmit("가".repeat(ReplyInputRules.MAX_LENGTH + 1)))
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadingIndicatorAppearsOnlyAfter500Millis() = runTest {
        val states = mutableListOf<Boolean>()
        val request = async {
            withDelayedLoading(states::add) {
                delay(API_LOADING_DELAY_MILLIS + 1)
            }
        }

        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(API_LOADING_DELAY_MILLIS - 1)
        assertTrue(states.isEmpty())
        testScheduler.advanceTimeBy(1)
        testScheduler.runCurrent()
        assertEquals(listOf(true), states)
        testScheduler.advanceTimeBy(1)
        request.await()

        assertEquals(listOf(true, false), states)
    }
}
