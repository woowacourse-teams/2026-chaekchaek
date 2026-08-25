package com.chaekchaek.app.ui.bookdetail

import com.chaekchaek.app.domain.rating.Rating
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
        assertTrue(shouldLockReview(80, 160, isSpoiler = false, spoilersRevealed = false))
        assertFalse(shouldLockReview(80, 160, isSpoiler = true, spoilersRevealed = true))
        assertEquals("짹짹 짹짹짹!\n짹짹?", maskAsChirps("감상 42쪽!\n좋다?"))
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
