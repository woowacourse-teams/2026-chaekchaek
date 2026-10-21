package com.chamsae.chaekchaek

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chaekchaek.app.data.remote.BookReview
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import com.chaekchaek.app.ui.bookdetail.BookDetailScreen
import com.chaekchaek.app.ui.bookdetail.BookDetailUiState
import com.chaekchaek.app.ui.bookdetail.ReviewPageLoadState
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDetailPaginationRetryTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun failedPageCanBeRetriedWithoutAutomaticRequestsOrDuplicateClicks() = verifyRetry(refresh = false)

    @Test
    fun failedRefreshKeepsRetryLoaderVisibleAndRecovers() = verifyRetry(refresh = true)

    private fun verifyRetry(refresh: Boolean) {
        val deleted = review(7, "삭제된 감상").copy(deleted = true)
        val state = mutableStateOf(
            BookDetailUiState(
                book = BookDetailArgs(id = "42", bookId = 42, title = "재시도 검증 도서"),
                reviews = if (refresh) listOf(review(6, "기존 감상")) else listOf(deleted),
                reviewCount = 2,
                nextReviewPage = if (refresh) null else 2,
                reviewPageLoadState = if (refresh) ReviewPageLoadState.RefreshFailed else ReviewPageLoadState.Failed,
            ),
        )
        var automaticRequests = 0
        var retryRequests = 0
        compose.setContent {
            ChaekchaekTheme {
                BookDetailScreen(
                    state = state.value,
                    onBack = {},
                    onLoadMoreReviews = { automaticRequests += 1 },
                    onRetryReviews = {
                        retryRequests += 1
                        state.value = state.value.copy(
                            reviewPageLoadState = if (refresh) ReviewPageLoadState.RefreshRetrying else ReviewPageLoadState.Retrying,
                        )
                    },
                )
            }
        }
        compose.onNodeWithText("감상을 불러오지 못했어요").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("다시 시도").performScrollTo().assertIsEnabled()
        compose.onRoot().performTouchInput { swipeUp() }
        compose.onNodeWithText("다시 시도").assertIsDisplayed()
        capture("${if (refresh) "refresh" else "pagination"}-failed")
        compose.onNodeWithText("다시 시도").performClick()
        compose.onNodeWithText("다시 시도").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(1, retryRequests)
            assertEquals(0, automaticRequests)
            state.value = state.value.copy(isLoading = refresh, isLoadingMore = !refresh)
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("감상 불러오는 중").assertIsDisplayed()
        capture("${if (refresh) "refresh" else "pagination"}-retrying")
        compose.runOnIdle {
            state.value = state.value.copy(
                isLoading = false,
                isLoadingMore = false,
                reviewPageLoadState = if (refresh) ReviewPageLoadState.RefreshFailed else ReviewPageLoadState.Failed,
            )
        }
        compose.onNodeWithText("다시 시도").assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(2, retryRequests)
            assertEquals(0, automaticRequests)
            state.value = state.value.copy(
                reviews = listOf(deleted, review(8, "재시도 성공 감상")),
                nextReviewPage = null,
                reviewPageLoadState = ReviewPageLoadState.Idle,
            )
        }
        compose.onNodeWithText("재시도 성공 감상").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("다시 시도").assertDoesNotExist()
        compose.onNodeWithText("삭제된 감상").assertDoesNotExist()
        compose.onNodeWithText("삭제된 감상입니다.").assertExists()
        capture("${if (refresh) "refresh" else "pagination"}-success")
        compose.runOnIdle { assertEquals(0, automaticRequests) }
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun review(id: Long, content: String) = BookReview(
        reviewId = id,
        content = content,
        quote = null,
        chapter = null,
        currentPage = null,
        createdAt = "2026-10-14T00:00:00Z",
        authorName = "검증 작성자",
        anonymous = false,
        replyCount = 0,
        likeCount = 0,
        writtenByMe = false,
    )
}
