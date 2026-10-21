package com.chamsae.chaekchaek

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chaekchaek.app.ui.bookdetail.BookQuestionPreviewScreen
import com.chaekchaek.app.ui.bookdetail.BookDetailReviewDraft
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookQuestionInteractionTest {
    @get:Rule val compose = createComposeRule()

    private val submitted = mutableListOf<BookDetailReviewDraft>()

    private fun launch(questionVisible: Boolean = true) {
        compose.setContent { ChaekchaekTheme {
            BookQuestionPreviewScreen(questionVisible = questionVisible, onReviewPreviewed = { submitted += it })
        } }
    }

    private fun scrollValue(tag: String): Float = compose.onNodeWithTag(tag)
        .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

    private fun revealQuestionControl(tag: String) {
        val control = compose.onNodeWithTag(tag).performScrollTo()
        repeat(4) {
            val footerTop = compose.onNodeWithText("이 순간의 감상 남기기", substring = true)
                .fetchSemanticsNode().boundsInRoot.top
            if (control.fetchSemanticsNode().boundsInRoot.bottom < footerTop) return
            // 스크롤이 안정된 뒤 컨트롤 전체가 보이는지 확인한다.
            compose.onNodeWithTag("book_detail_list").performTouchInput {
                swipe(start = androidx.compose.ui.geometry.Offset(4f, height * .7f),
                    end = androidx.compose.ui.geometry.Offset(4f, height * .4f), durationMillis = 400)
            }
        }
        control.assertIsDisplayed()
    }

    @Test
    fun reviewFiltersKeepFullTouchAreaAboveComposerAndSwitchScope() {
        launch()
        val list = compose.onNodeWithTag("book_detail_list")
        list.performScrollToNode(hasTestTag("review_scope_all"))
        val all = compose.onNodeWithTag("review_scope_all").performScrollTo()
        val mine = compose.onNodeWithTag("review_scope_mine")
        val listBounds = list.fetchSemanticsNode().boundsInRoot
        val footerTop = compose.onNodeWithText("이 순간의 감상 남기기", substring = true)
            .fetchSemanticsNode().boundsInRoot.top
        assertTrue("목록 뷰포트가 고정 작성 버튼과 겹치면 안 된다", listBounds.bottom <= footerTop)
        for (filter in listOf(all, mine)) {
            filter.assertIsDisplayed()
            val bounds = filter.fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.width >= with(compose.density) { 44.dp.toPx() - 1f })
            assertTrue(bounds.height >= with(compose.density) { 44.dp.toPx() - 1f })
            assertTrue(bounds.bottom <= listBounds.bottom)
        }
        mine.performTouchInput { click() }.assertIsSelected()
        all.performTouchInput { click() }.assertIsSelected()
    }

    @Test
    fun overlayKeepsListPositionAndAllowsIndependentAndBackgroundScrolling() {
        launch()
        revealQuestionControl("question_reference_open")
        compose.onNodeWithTag("question_reference_open").performClick()
        val listBefore = scrollValue("book_detail_list")
        val overlayHeight = compose.onNodeWithTag("question_reference_overlay").fetchSemanticsNode().boundsInRoot.height
        assertTrue(overlayHeight <= with(compose.density) { 340.dp.toPx() + 1f })
        compose.onNodeWithTag("question_reference_overlay").performTouchInput {
            swipe(start = androidx.compose.ui.geometry.Offset(2f, height * .75f),
                end = androidx.compose.ui.geometry.Offset(2f, height * .25f), durationMillis = 600)
        }
        assertEquals(listBefore, scrollValue("book_detail_list"), 0.01f)
        compose.onNodeWithTag("question_reference_body").performTouchInput { swipeUp() }
        assertTrue(scrollValue("question_reference_body") > 0f)
        compose.onNodeWithTag("question_tab_Excerpt").performClick().assertIsSelected()
        assertEquals(listBefore, scrollValue("book_detail_list"), 0.01f)
        compose.onNodeWithTag("question_reference_close").performClick()
        assertEquals(listBefore, scrollValue("book_detail_list"), 0.01f)
        compose.onNodeWithTag("question_reference_open").performClick()
        val listTop = compose.onNodeWithTag("book_detail_list").fetchSemanticsNode().boundsInRoot.top
        val panelBottom = compose.onNodeWithTag("question_reference_overlay").fetchSemanticsNode().boundsInRoot.bottom
        val footerTop = compose.onNodeWithText("이 순간의 감상 남기기", substring = true)
            .fetchSemanticsNode().boundsInRoot.top
        val startY = footerTop - listTop - with(compose.density) { 24.dp.toPx() }
        val endY = panelBottom - listTop + with(compose.density) { 16.dp.toPx() }
        assertTrue("패널과 하단 버튼 사이에 목록 스크롤 영역이 있어야 한다", startY > endY)
        compose.onNodeWithTag("book_detail_list").performTouchInput {
            swipe(start = androidx.compose.ui.geometry.Offset(centerX, startY),
                end = androidx.compose.ui.geometry.Offset(centerX, endY), durationMillis = 600)
        }
        assertTrue(scrollValue("book_detail_list") > listBefore)
        compose.onNodeWithTag("question_reference_overlay").assertIsDisplayed()
    }

    @Test
    fun noQuestionHidesQuestionSectionAndReadingRecord() {
        launch(questionVisible = false)
        compose.onNodeWithTag("book_question").assertDoesNotExist()
        compose.onNodeWithTag("question_reference_open").assertDoesNotExist()
        compose.onNodeWithText("내 독서 기록").assertDoesNotExist()
        compose.onNodeWithText("이 순간의 감상 남기기", substring = true).assertIsDisplayed()
    }

    @Test
    fun previewDraftSurvivesContinueWriting() {
        launch()
        revealQuestionControl("question_review_open")
        compose.onNodeWithTag("question_review_open").performClick()
        compose.onNodeWithContentDescription("감상 (필수)").performTextInput("계속 쓰고 싶은 감상")
        compose.onNodeWithContentDescription("인상 깊은 문구").performScrollTo().performTextInput("남겨둘 문구")
        compose.onNodeWithContentDescription("닫기").performScrollTo().performClick()
        compose.onNodeWithText("계속 작성").performClick()
        compose.onNodeWithContentDescription("감상 (필수)").assertTextEquals("계속 쓰고 싶은 감상")
        compose.onNodeWithContentDescription("인상 깊은 문구").assertTextEquals("남겨둘 문구")
        compose.runOnIdle { assertTrue(submitted.isEmpty()) }
    }

    @Test
    fun emojiCanBeSelectedAndCancelledAndPreviewReviewWithZeroCanBeSubmitted() {
        launch()
        revealQuestionControl("question_reaction")
        compose.onNodeWithTag("question_reaction").performClick()
        compose.onNodeWithTag("question_reaction_picker").assertIsDisplayed()
        compose.onNodeWithTag("question_reaction_choice_ThumbsUp").performClick()
        compose.onNodeWithTag("question_reaction_picker").assertDoesNotExist()
        compose.onNodeWithTag("question_reaction").assertIsDisplayed()
        compose.onNodeWithTag("question_reaction_selected").performClick().assertDoesNotExist()
        compose.onNodeWithTag("question_review_open").performClick()
        compose.onNodeWithContentDescription("별점 선택").assertExists()
        compose.onNodeWithContentDescription("쪽수").assertDoesNotExist()
        compose.onNodeWithContentDescription("목차 또는 챕터").assertDoesNotExist()
        compose.onNodeWithContentDescription("감상 (필수)").performTextInput("시간을 들인 관계를 떠올렸어요.")
        compose.onNode(hasText("감상 남기기") and hasClickAction()).performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onNodeWithTag("question_preview_confirmation").isDisplayed()
        }
        compose.onNodeWithTag("question_preview_confirmation").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(1, submitted.size)
            assertEquals(0f, submitted.single().rating)
            assertEquals("시간을 들인 관계를 떠올렸어요.", submitted.single().content)
        }
    }
}
