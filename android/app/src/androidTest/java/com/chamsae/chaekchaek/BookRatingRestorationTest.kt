package com.chamsae.chaekchaek

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chaekchaek.app.data.remote.BookDetail
import com.chaekchaek.app.data.remote.LibraryRecord
import com.chaekchaek.app.domain.rating.Rating
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

    private fun verifyRating(edited: Boolean, restore: Boolean) {
        val state = mutableStateOf(
            BookDetailUiState(
                book = BookDetailArgs(id = "42", bookId = 42, title = "별점 복원 검증"),
                signedIn = true,
            ),
        )
        var saved: Rating? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            ChaekchaekTheme {
                BookDetailScreen(state = state.value, onBack = {}, onRatingSave = { rating, _ -> saved = rating })
            }
        }
        compose.onNodeWithText("별점 주기").performScrollTo().performClick()
        if (edited) compose.onNodeWithContentDescription("0.5점 높이기").performClick()
        if (restore) restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle { state.value = state.value.copy(detail = detailWithRating(2.0)) }
        compose.onNodeWithText("별점 저장").performClick()
        compose.runOnIdle { assertEquals(if (edited) 4.5 else 2.0, saved?.score?.toDouble()) }
    }

    private fun detailWithRating(rating: Double) = BookDetail(
        bookId = 42, isbn13 = "", title = "별점 복원 검증", authors = emptyList(),
        translators = emptyList(), publisher = "", category = "", publishedDate = null,
        totalPages = 100, coverImageUrl = "", commentCount = 0, averageRating = null,
        ratingCount = 0, myRecord = LibraryRecord(status = "READING", currentPage = 0, rating = rating),
    )
}
