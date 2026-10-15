package com.chamsae.chaekchaek

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chaekchaek.app.data.remote.BookReview
import com.chaekchaek.app.data.remote.ReviewScope
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import com.chaekchaek.app.ui.bookdetail.BookDetailScreen
import com.chaekchaek.app.ui.bookdetail.BookDetailUiState
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDetailScrollRestorationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun restoredMemberKeepsItsListPosition() = verifyPosition(restoredMember = true)

    @Test
    fun failedMemberRestorationStartsGuestListAtTop() = verifyPosition(restoredMember = false)

    private fun verifyPosition(restoredMember: Boolean) {
        // 일반 변수는 복원 전 재구성을 일으키지 않아, 저장 당시 회원 상태와
        // 새 Composition의 인증 복원 결과를 따로 재현한다.
        var signedIn = true
        val reviews = (1L..12L).map { id ->
            BookReview(reviewId = id, content = "복원 감상 $id", quote = null, chapter = null,
                currentPage = null, createdAt = "2026-10-15T00:00:00Z", authorName = "작성자",
                anonymous = false, replyCount = 0, likeCount = 0, writtenByMe = false)
        }
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            ChaekchaekTheme {
                BookDetailScreen(
                    state = BookDetailUiState(
                        book = BookDetailArgs(id = "42", bookId = 42, title = "스크롤 복원 검증"),
                        signedIn = signedIn, reviews = reviews, reviewCount = reviews.size,
                        reviewScope = if (signedIn) ReviewScope.MINE else ReviewScope.ALL,
                    ),
                    onBack = {},
                )
            }
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(3)
        assertTrue(scrollPosition() > 0f)
        signedIn = restoredMember
        restoration.emulateSavedInstanceStateRestore()
        if (restoredMember) assertTrue(scrollPosition() > 0f)
        else assertEquals(0f, scrollPosition(), 0f)
    }

    private fun scrollPosition(): Float = compose.onNode(hasScrollToIndexAction())
        .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
}
