package com.chaekchaek.app.ui.bookdetail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BookQuestionAReviewRulesTest {
    @Test
    fun unselectedAndActualZeroRemainDifferentDrafts() {
        val unselected = QuestionAReviewDraft(content = "감상")
        val zero = unselected.copy(rating = 0.0)
        assertNull(QuestionAReviewRules.normalize(unselected).rating)
        assertEquals(0.0, QuestionAReviewRules.normalize(zero).rating)
        assertNotEquals(unselected, zero)
        assertTrue(QuestionAReviewRules.hasChanges(zero, unselected))
        assertTrue(QuestionAReviewRules.canSubmit(unselected))
        assertTrue(QuestionAReviewRules.canSubmit(zero))
    }

    @Test
    fun previewSelectionIncludesZeroAndServerSelectionStartsAtHalfStar() {
        assertEquals(0.0, QuestionAReviewRules.ratingAtFraction(0f))
        assertEquals(0.0, QuestionAReviewRules.ratingAtProgress(0f))
        assertEquals(0.5, QuestionAReviewRules.ratingAtFraction(0f, allowZeroRating = false))
        assertEquals(0.5, QuestionAReviewRules.ratingAtProgress(0f, allowZeroRating = false))
        assertEquals(5.0, QuestionAReviewRules.ratingAtFraction(1f))
        assertEquals(5.0, QuestionAReviewRules.ratingAtFraction(1f, allowZeroRating = false))
        assertEquals(4.0, QuestionAReviewRules.ratingAtProgress(4.2f))
        assertEquals(4.5, QuestionAReviewRules.ratingAtProgress(4.3f))
    }

    @Test
    fun oldFractionalRatingIsPreservedUntilTheUserSelects() {
        val draft = QuestionAReviewDraft(content = "보존할 감상", rating = 4.2)
        assertEquals(4.2, QuestionAReviewRules.normalize(draft).rating)
        assertFalse(QuestionAReviewRules.hasChanges(draft, draft))
        assertNull(QuestionAReviewRules.normalize(draft.copy(rating = Double.NaN)).rating)
        assertNull(QuestionAReviewRules.normalize(draft.copy(rating = -0.5)).rating)
        assertNull(QuestionAReviewRules.normalize(draft.copy(rating = 5.5)).rating)
    }

    @Test
    fun initialAndRestoredTextUseTheExistingUtf16BoundsWithoutSplittingEmoji() {
        val draft = QuestionAReviewRules.normalize(QuestionAReviewDraft(
            content = "가".repeat(999) + "😀" + "뒤",
            quote = "나".repeat(499) + "😀" + "뒤",
        ))
        assertEquals("가".repeat(999), draft.content)
        assertEquals("나".repeat(499), draft.quote)
        assertTrue(QuestionAReviewRules.canSubmit(draft))
        assertFalse(QuestionAReviewRules.canSubmit(draft.copy(content = " \n\t")))
        assertFalse(QuestionAReviewRules.canSubmit(draft.copy(content = "가".repeat(1001))))
        assertFalse(QuestionAReviewRules.canSubmit(draft.copy(quote = "나".repeat(501))))
    }

    @Test
    fun changesToOptionalFieldsRequireDiscardConfirmation() {
        val initial = QuestionAReviewDraft()
        assertFalse(QuestionAReviewRules.hasChanges(initial, initial))
        assertTrue(QuestionAReviewRules.hasChanges(initial.copy(quote = "문구"), initial))
        assertTrue(QuestionAReviewRules.hasChanges(initial.copy(isSpoiler = true), initial))
        assertTrue(QuestionAReviewRules.hasChanges(initial.copy(rating = 0.0), initial))
    }
}
