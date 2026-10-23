package com.chamsae.chaekchaek

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chaekchaek.app.data.remote.BookDetail
import com.chaekchaek.app.data.remote.LibraryRecord
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import com.chaekchaek.app.ui.bookdetail.BookDetailScreen
import com.chaekchaek.app.ui.bookdetail.BookDetailUiState
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookRatingRestorationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun lateExistingRatingReplacesUntouchedDefault() = verifyRating(edited = false, restore = false)

    @Test
    fun lateExistingRatingDoesNotReplaceUserEdit() = verifyRating(edited = true, restore = false)

    @Test
    fun restoredUserEditSurvivesLateExistingRating() = verifyRating(edited = true, restore = true)

    @Test
    fun restoredUntouchedDraftAcceptsLateExistingFractionalRating() =
        verifyRating(edited = false, restore = true, existingRating = 4.2)

    @Test
    fun ratingWithoutLibraryRecordPrefillsDraft() =
        verifyRating(edited = false, restore = false, existingRating = 4.5, ratingOnly = true)

    @Test
    fun ratingWithoutLibraryRecordSurvivesDraftRestoration() =
        verifyRating(edited = false, restore = true, existingRating = 4.5, ratingOnly = true)

    private fun verifyRating(
        edited: Boolean,
        restore: Boolean,
        existingRating: Double = 2.0,
        ratingOnly: Boolean = false,
    ) {
        val state = mutableStateOf(
            BookDetailUiState(
                book = BookDetailArgs(id = "42", bookId = 42, title = "별점 복원 검증"),
                signedIn = true,
            ),
        )
        var saved: Double? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            ChaekchaekTheme {
                BookDetailScreen(
                    state = state.value,
                    onBack = {},
                    onQuestionAReviewCreate = { draft -> saved = draft.rating },
                )
            }
        }
        compose.onNodeWithText("이 순간의 감상 남기기", substring = true).performClick()
        compose.onNodeWithContentDescription("감상 (필수)").performTextInput("별점 복원 검증 감상")
        compose.onNodeWithContentDescription("별점 (선택)").performScrollTo().assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "미선택"),
        )
        if (edited) compose.onNodeWithContentDescription("별점 (선택)")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(4.5f) }
        if (restore) restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            val detail = detailWithRating(existingRating)
            state.value = state.value.copy(detail = if (ratingOnly) {
                detail.copy(myRecord = null, myRating = existingRating, isRegisteredInMyLibrary = false)
            } else detail)
        }
        val expected = if (edited) 4.5 else existingRating
        compose.onNodeWithContentDescription("별점 (선택)").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "${expected}점"),
        )
        compose.onNode(hasText("감상 남기기") and hasClickAction()).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(expected, saved) }
    }

    private fun detailWithRating(rating: Double) = BookDetail(
        bookId = 42, isbn13 = "", title = "별점 복원 검증", authors = emptyList(),
        translators = emptyList(), publisher = "", category = "", publishedDate = null,
        totalPages = 100, coverImageUrl = "", commentCount = 0, averageRating = null,
        ratingCount = 0, myRecord = LibraryRecord(status = "READING", currentPage = 0, rating = rating),
    )
}
