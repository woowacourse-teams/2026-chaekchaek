package com.chamsae.chaekchaek

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chaekchaek.app.data.remote.BookDetail
import com.chaekchaek.app.data.remote.BookReview
import com.chaekchaek.app.data.remote.LibraryRecord
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import com.chaekchaek.app.ui.bookdetail.BookDetailAuthenticatedAction
import com.chaekchaek.app.ui.bookdetail.BookDetailScreen
import com.chaekchaek.app.ui.bookdetail.BookDetailUiState
import com.chaekchaek.app.ui.bookdetail.BookQuestionUiModel
import com.chaekchaek.app.ui.bookdetail.QuestionAReviewDraft
import com.chaekchaek.app.ui.bookdetail.ReviewSubmissionState
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookQuestionAUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun referenceTabsAndIndependentBodyScrollKeepDetailPosition() {
        compose.setContent {
            ChaekchaekTheme {
                BookDetailScreen(state = detailState(), question = question(), onBack = {})
            }
        }
        compose.onNodeWithTag("questionReferenceOpen").performScrollTo()
        val position = detailPosition()
        compose.onNodeWithTag("questionReferenceOpen").performClick()
        compose.onNodeWithTag("questionReferencePanel").assertIsDisplayed()
        assertEquals(position, detailPosition(), 0.0001f)

        compose.onNodeWithTag("questionReferenceTabSynopsis").assertIsSelected()
        assertEquals(0f, referencePosition(), 0f)
        capture("reference", compose.onNodeWithTag("questionReferencePanel"))
        compose.onNodeWithTag("book-question-reference-body").performTouchInput { swipeUp() }
        assertTrue(referencePosition() > 0f)
        val synopsisPosition = referencePosition()
        assertEquals(position, detailPosition(), 0.0001f)

        compose.onNodeWithTag("questionReferenceTabExcerpt").performClick().assertIsSelected()
        assertEquals(0f, referencePosition(), 0f)
        compose.onNodeWithTag("book-question-reference-body").performTouchInput { swipeUp() }
        assertTrue(referencePosition() > 0f)
        assertEquals(position, detailPosition(), 0.0001f)

        compose.onNodeWithTag("questionReferenceTabSynopsis").performClick().assertIsSelected()
        assertEquals(synopsisPosition, referencePosition(), 0.1f)
        compose.onNodeWithContentDescription("질문과 장면 닫기").performClick()
        compose.onNodeWithTag("questionReferencePanel").assertDoesNotExist()
        assertEquals(position, detailPosition(), 0.0001f)
    }

    @Test
    fun referencePanelPaddingDoesNotActivateCoveredBackButton() {
        var backCalls = 0
        compose.setContent {
            ChaekchaekTheme {
                BookDetailScreen(state = detailState(), question = question(), onBack = { backCalls += 1 })
            }
        }
        val list = compose.onNodeWithTag("book_detail_list")
        list.performScrollToNode(hasTestTag("questionReferenceOpen"))
        compose.onNodeWithTag("questionReferenceOpen").performScrollTo().assertIsDisplayed().performClick()
        list.performScrollToIndex(0)
        val panel = compose.onNodeWithTag("questionReferencePanel")
        val inset = with(compose.density) { 8.dp.toPx() }
        val tap = Offset(inset, inset)
        val panelBounds = panel.fetchSemanticsNode().boundsInRoot
        val backBounds = compose.onNodeWithTag("book_detail_back").fetchSemanticsNode().boundsInRoot
        assertTrue("검증 탭은 패널 여백과 가려진 뒤로가기 버튼에 함께 놓여야 한다", backBounds.contains(panelBounds.topLeft + tap))

        panel.performTouchInput { click(tap) }
        compose.runOnIdle { assertEquals(0, backCalls) }
        panel.assertIsDisplayed()
        compose.onNodeWithTag("questionReferenceTabExcerpt").performClick().assertIsSelected()
        compose.onNodeWithContentDescription("질문과 장면 닫기").performClick()
        panel.assertDoesNotExist()
    }

    @Test
    fun referenceOverlayLeavesBackgroundReviewListTouchable() {
        compose.setContent {
            ChaekchaekTheme {
                BookDetailScreen(state = detailState(), question = question(), onBack = {})
            }
        }
        compose.onNodeWithTag("questionReferenceOpen").performScrollTo().performClick()
        val before = detailPosition()
        val listBounds = compose.onNode(hasScrollToIndexAction()).fetchSemanticsNode().boundsInRoot
        val panelBottom = compose.onNodeWithTag("questionReferencePanel").fetchSemanticsNode().boundsInRoot.bottom
        val footerTop = compose.onNodeWithText("이 순간의 감상 남기기", substring = true)
            .fetchSemanticsNode().boundsInRoot.top
        val start = footerTop - listBounds.top - 24f
        val end = maxOf(panelBottom - listBounds.top + 24f, start - 240f)
        assertTrue("패널과 하단 버튼 사이에 실제 스와이프 영역이 있어야 한다", start - end > 48f)
        compose.onNode(hasScrollToIndexAction()).performTouchInput {
            swipe(Offset(centerX, start), Offset(centerX, end), durationMillis = 400)
        }
        compose.waitForIdle()
        assertTrue("패널 밖의 실제 터치가 감상 목록을 스크롤해야 한다", detailPosition() > before)
        compose.onNodeWithTag("questionReferencePanel").assertIsDisplayed()
        val scrolled = detailPosition()
        compose.onNodeWithContentDescription("질문과 장면 닫기").performClick()
        assertEquals(scrolled, detailPosition(), 0.0001f)
    }

    @Test
    fun bookWithoutQuestionOmitsQuestionAndReadingRecordControls() {
        compose.setContent {
            ChaekchaekTheme {
                BookDetailScreen(state = detailState(withRecord = true), question = null, onBack = {})
            }
        }
        compose.onNodeWithText("여운 한 줄").assertDoesNotExist()
        compose.onNodeWithTag("questionReferenceOpen").assertDoesNotExist()
        compose.onNodeWithText("내 독서 기록").assertDoesNotExist()
        compose.onNodeWithText("별점 주기").assertDoesNotExist()
        compose.onNodeWithText("이 순간의 감상 남기기", substring = true).assertIsDisplayed()
        capture("no-question", compose.onNode(hasScrollToIndexAction()))
    }

    @Test
    fun failedReviewKeepsContentQuoteAndExplicitZeroAcrossRestoration() {
        val state = mutableStateOf(detailState())
        val drafts = mutableListOf<QuestionAReviewDraft>()
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            ChaekchaekTheme {
                BookDetailScreen(
                    state = state.value, question = question(), onBack = {}, allowZeroReviewRating = true,
                    onQuestionAReviewCreate = { draft ->
                        drafts += draft
                        state.value = state.value.copy(reviewSubmissionState = ReviewSubmissionState.Submitting)
                    },
                )
            }
        }
        openReview()
        enterDraft()
        rating().performScrollTo().assert(ratingDescription("미선택"))
        rating().performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        rating().assert(ratingDescription("0.0점"))
        capture("composer-zero", compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.PaneTitle)))
        submitReview()
        compose.runOnIdle {
            assertEquals(QuestionAReviewDraft(DRAFT_CONTENT, DRAFT_QUOTE, rating = 0.0), drafts.single())
            state.value = state.value.copy(reviewSubmissionState = ReviewSubmissionState.Failed)
        }
        restoration.emulateSavedInstanceStateRestore()
        assertDraft()
        rating().assert(ratingDescription("0.0점"))
        compose.onNodeWithContentDescription("목차 또는 챕터").assertDoesNotExist()
        compose.onNodeWithContentDescription("쪽수").assertDoesNotExist()
        submitReview()
        compose.runOnIdle {
            assertEquals(2, drafts.size)
            assertEquals(drafts.first(), drafts.last())
        }
    }

    @Test
    fun untouchedRatingRemainsNullWhenReviewIsSubmitted() {
        var submitted: QuestionAReviewDraft? = null
        compose.setContent {
            ChaekchaekTheme {
                BookDetailScreen(
                    state = detailState(), question = question(), onBack = {}, allowZeroReviewRating = true,
                    onQuestionAReviewCreate = { submitted = it },
                )
            }
        }
        openReview()
        compose.onNodeWithContentDescription("감상 (필수)").performTextInput(DRAFT_CONTENT)
        rating().performScrollTo().assert(ratingDescription("미선택"))
        submitReview()
        compose.waitUntil(timeoutMillis = 5_000) { submitted != null }
        compose.runOnIdle { assertEquals(QuestionAReviewDraft(content = DRAFT_CONTENT), submitted) }
    }

    @Test
    fun guestRatingLoginRoundTripKeepsDraftAndRequiresExplicitRatingSelection() {
        val state = mutableStateOf(detailState().copy(signedIn = false))
        val resumedAction = mutableStateOf<BookDetailAuthenticatedAction?>(null)
        val loginRequests = mutableListOf<BookDetailAuthenticatedAction>()
        var handled = 0
        var submitted: QuestionAReviewDraft? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            ChaekchaekTheme {
                BookDetailScreen(
                    state = state.value, question = question(), onBack = {}, allowZeroReviewRating = true,
                    resumedAuthenticatedAction = resumedAction.value,
                    onAuthenticatedActionHandled = { handled++; resumedAction.value = null },
                    onLoginRequired = { loginRequests += it },
                    onQuestionAReviewCreate = { submitted = it },
                )
            }
        }
        openReview()
        enterDraft()
        rating().performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(4f) }
        compose.runOnIdle { assertEquals(listOf(BookDetailAuthenticatedAction.OpenReviewRating), loginRequests) }
        assertDraft()
        rating().assert(ratingDescription("미선택"))
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            state.value = state.value.copy(signedIn = true)
            resumedAction.value = BookDetailAuthenticatedAction.OpenReviewRating
        }
        assertDraft()
        rating().assert(ratingDescription("미선택"))
        compose.runOnIdle { assertEquals(1, handled) }
        rating().performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(4f) }
        rating().assert(ratingDescription("4.0점"))
        submitReview()
        compose.runOnIdle { assertEquals(QuestionAReviewDraft(DRAFT_CONTENT, DRAFT_QUOTE, rating = 4.0), submitted) }
    }

    @Test
    fun continueWritingAfterScrimAccessibilityDismissShowsTheSameDraftAgain() {
        var keyboard: SoftwareKeyboardController? = null
        compose.setContent {
            keyboard = LocalSoftwareKeyboardController.current
            ChaekchaekTheme {
                BookDetailScreen(state = detailState(), question = question(), onBack = {})
            }
        }
        openReview()
        enterDraft()
        compose.runOnIdle { keyboard?.hide() }
        compose.waitForIdle()

        val closeSheetLabel = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(androidx.compose.ui.R.string.close_sheet)
        // Scrim은 시트 뒤의 전체 화면이므로 중앙 터치는 입력란에 가려진다.
        // 접근성 닫기 동작으로 Material3의 동일한 dismiss 애니메이션 경로를 실행한다.
        compose.onNodeWithContentDescription(closeSheetLabel)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText("감상 작성을 그만둘까요?")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("감상 작성을 그만둘까요?").assertIsDisplayed()
        compose.onNode(hasText("계속 작성") and hasClickAction()).performClick()
        compose.onNodeWithText("감상 작성을 그만둘까요?").assertDoesNotExist()
        compose.onNodeWithContentDescription("감상 (필수)").performScrollTo().assertIsDisplayed()
            .assertTextEquals(DRAFT_CONTENT)
        compose.onNodeWithContentDescription("인상 깊은 문구 (선택)").performScrollTo().assertIsDisplayed()
            .assertTextEquals(DRAFT_QUOTE)
    }

    @Test
    fun referencesUseOnlyAvailableTabsAndEmojiControlsAreAbsent() {
        val currentQuestion = mutableStateOf(question().copy(synopsisMarkdown = ""))
        compose.setContent {
            ChaekchaekTheme {
                BookDetailScreen(state = detailState(), question = currentQuestion.value, onBack = {})
            }
        }
        compose.onNodeWithTag("questionReactionOpen").assertDoesNotExist()
        compose.onNodeWithTag("questionReferenceOpen").performScrollTo().performClick()
        compose.onNodeWithTag("questionReferenceTabSynopsis").assertDoesNotExist()
        compose.onNodeWithTag("questionReferenceTabExcerpt").assertIsSelected()
        compose.onNodeWithTag("questionReferenceClose").performClick()
        compose.runOnIdle { currentQuestion.value = question().copy(excerptMarkdown = "") }
        compose.onNodeWithTag("questionReferenceOpen").performScrollTo().performClick()
        compose.onNodeWithTag("questionReferenceTabExcerpt").assertDoesNotExist()
        compose.onNodeWithTag("questionReferenceTabSynopsis").assertIsSelected()
        // A refresh can remove the selected material while the overlay is open.
        compose.runOnIdle { currentQuestion.value = question().copy(synopsisMarkdown = "", excerptMarkdown = "") }
        compose.onNodeWithTag("questionReferencePanel").assertDoesNotExist()
        compose.onNodeWithTag("questionReferenceOpen").assertDoesNotExist()
        compose.onNodeWithTag("questionReviewOpen").performScrollTo().assertIsDisplayed()
    }

    private fun openReview() {
        compose.onNodeWithText("이 순간의 감상 남기기", substring = true).performClick()
        compose.onNodeWithContentDescription("감상 (필수)").assertExists()
    }

    private fun capture(name: String, node: SemanticsNodeInteraction) {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "question-a-$name.png")
        val bitmap = node.captureToImage().asAndroidBitmap()
        file.outputStream().use { stream ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }

    private fun enterDraft() {
        compose.onNodeWithContentDescription("감상 (필수)").performTextInput(DRAFT_CONTENT)
        compose.onNodeWithContentDescription("인상 깊은 문구 (선택)").performScrollTo().performTextInput(DRAFT_QUOTE)
    }

    private fun assertDraft() {
        compose.onNodeWithContentDescription("감상 (필수)").assertTextEquals(DRAFT_CONTENT)
        compose.onNodeWithContentDescription("인상 깊은 문구 (선택)").assertTextEquals(DRAFT_QUOTE)
    }

    private fun rating() = compose.onNodeWithContentDescription("별점 (선택)")

    private fun ratingDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    private fun submitReview() {
        compose.onNode(hasText("감상 남기기") and hasClickAction()).performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
    }

    private fun detailPosition(): Float = compose.onNode(hasScrollToIndexAction())
        .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

    private fun referencePosition(): Float {
        compose.waitUntil(timeoutMillis = 5_000) {
            val bodies = compose.onAllNodesWithTag("book-question-reference-body").fetchSemanticsNodes()
            bodies.size == 1 && bodies.single().config[SemanticsProperties.VerticalScrollAxisRange].maxValue() > 0f
        }
        return compose.onNodeWithTag("book-question-reference-body")
            .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
    }

    private fun question() = BookQuestionUiModel(
        id = "instrumentation-question",
        questionMarkdown = "이 장면에서 어떤 **선택**을 했을까요?",
        synopsisMarkdown = (1..30).joinToString("\n\n") { "줄거리 검증 문단 $it. 인물이 다음 장면을 향해 걸어갑니다." },
        excerptMarkdown = (1..30).joinToString("\n\n") { "> 본문 발췌 검증 문장 $it. 기억하고 싶은 장면입니다." },
    )

    private fun detailState(withRecord: Boolean = false): BookDetailUiState {
        val reviews = (1L..20L).map { id ->
            BookReview(
                reviewId = id, content = "질문 A 스크롤 검증 감상 $id", quote = null, chapter = null,
                currentPage = null, createdAt = "2026-10-21T00:00:00Z", authorName = "작성자",
                anonymous = false, replyCount = 0, likeCount = 0,
            )
        }
        return BookDetailUiState(
            book = BookDetailArgs(id = "42", bookId = 42, title = "질문 A UI 검증", totalPages = 224),
            detail = if (withRecord) BookDetail(
                bookId = 42, isbn13 = "", title = "질문 A UI 검증", authors = emptyList(), translators = emptyList(),
                publisher = "", category = "", publishedDate = null, totalPages = 224, coverImageUrl = "",
                commentCount = reviews.size, averageRating = null, ratingCount = 0,
                myRecord = LibraryRecord(status = "READING", currentPage = 37, rating = 2.0),
            ) else null,
            signedIn = true, reviews = reviews, reviewCount = reviews.size,
        )
    }

    private companion object {
        const val DRAFT_CONTENT = "로그인과 저장 실패 후에도 남아야 하는 감상"
        const val DRAFT_QUOTE = "기억하고 싶은 문장"
    }
}
