package com.chamsae.chaekchaek

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chaekchaek.app.data.remote.ReviewCreateRequest
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import com.chaekchaek.app.ui.bookdetail.BookDetailScreen
import com.chaekchaek.app.ui.bookdetail.BookDetailUiState
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReviewDraftPreservationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun failedSubmissionKeepsEveryFieldForRetryWithoutReadingRecord() {
        val state = mutableStateOf(BookDetailUiState(
            book = BookDetailArgs(id = "42", bookId = 42, title = "감상 보존 검증", totalPages = 224),
            signedIn = true,
        ))
        val requests = mutableListOf<ReviewCreateRequest>()
        compose.setContent {
            ChaekchaekTheme {
                BookDetailScreen(
                    state = state.value,
                    onBack = {},
                    onReviewCreate = { request ->
                        requests += request
                        state.value = state.value.copy(requestError = "요청을 처리하지 못했어요. 다시 시도해 주세요.")
                    },
                    onRequestErrorShown = { state.value = state.value.copy(requestError = null) },
                )
            }
        }
        compose.onNodeWithText("감상 남기기").performClick()
        compose.onNodeWithContentDescription("감상 (필수)").performTextInput("사라지면 안 되는 감상")
        compose.onNodeWithContentDescription("인상 깊은 문구").performScrollTo().performTextInput("기억할 문장")
        compose.onNodeWithContentDescription("목차/챕터").performScrollTo().performTextInput("첫 번째 장")
        compose.onNodeWithContentDescription("쪽수").performScrollTo().performTextInput("80")
        compose.onNodeWithText("스포일러 포함").performScrollTo().performClick()
        compose.onAllNodes(hasText("감상 남기기") and hasClickAction()).onLast().performScrollTo().performClick()

        compose.onNodeWithContentDescription("감상 (필수)").assertTextEquals("사라지면 안 되는 감상")
        compose.onNodeWithContentDescription("인상 깊은 문구").assertTextEquals("기억할 문장")
        compose.onNodeWithContentDescription("목차/챕터").assertTextEquals("첫 번째 장")
        compose.onNodeWithContentDescription("쪽수").assertTextEquals("80")
        compose.runOnIdle {
            assertEquals(listOf(ReviewCreateRequest(
                content = "사라지면 안 되는 감상", quote = "기억할 문장", chapter = "첫 번째 장",
                currentPage = 80, totalPages = 224, isSpoiler = true,
            )), requests)
        }
    }
}
