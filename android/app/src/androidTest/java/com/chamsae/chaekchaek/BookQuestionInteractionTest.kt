package com.chamsae.chaekchaek

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.view.accessibility.AccessibilityNodeInfo
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
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val submitted = mutableListOf<BookDetailReviewDraft>()

    private fun launch(questionVisible: Boolean = true, landscape: Boolean = false) {
        compose.runOnUiThread {
            compose.activity.requestedOrientation = if (landscape) {
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
        compose.setContent { ChaekchaekTheme {
            BookQuestionPreviewScreen(questionVisible = questionVisible, onReviewPreviewed = { submitted += it })
        } }
        compose.waitUntil(timeoutMillis = 5_000) {
            val viewport = compose.onNodeWithTag("book_detail_list").fetchSemanticsNode().boundsInRoot
            (viewport.width > viewport.height) == landscape
        }
    }

    private fun scrollValue(tag: String): Float = compose.onNodeWithTag(tag)
        .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

    private fun revealQuestionControl(tag: String) {
        compose.onNodeWithTag("book_detail_list").performScrollToNode(hasTestTag(tag))
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
    fun libraryButtonReturnsToAccessibilityAndSavesAfterReferenceCloses() {
        launch()
        val list = compose.onNodeWithTag("book_detail_list")
        val library = compose.onNodeWithTag("book_detail_library_save").performScrollTo()
        val hidden = SemanticsMatcher.keyIsDefined(SemanticsProperties.HideFromAccessibility)
        library.assert(hidden.not()).assertIsDisplayed()
        revealQuestionControl("question_reference_open")
        compose.onNodeWithTag("question_reference_open").performClick()
        // LazyColumn이 항목을 제거한 경우도 접근성 탐색에는 노출되지 않는다.
        compose.onAllNodes(hasTestTag("book_detail_library_save")).fetchSemanticsNodes().forEach {
            assertTrue(it.config.contains(SemanticsProperties.HideFromAccessibility))
        }
        compose.onNodeWithTag("question_reference_close").performClick()
        list.performScrollToIndex(0)
        library.performScrollTo().assert(hidden.not()).assertIsDisplayed()
        val bounds = library.fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.height >= with(compose.density) { 44.dp.toPx() - 1f })
        library.performTouchInput { click() }
        compose.onNodeWithText("담았어요").assertIsDisplayed()
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

    private fun assertNativeAccessibleControl(label: String, expected: Boolean) {
        compose.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.waitForIdle(200, 5_000)
        var root = checkNotNull(automation.rootInActiveWindow) { "실제 Android 접근성 트리를 읽을 수 없다" }
        fun hasLabel(node: AccessibilityNodeInfo): Boolean =
            node.text?.toString() == label || node.contentDescription?.toString() == label ||
                (0 until node.childCount).any { node.getChild(it)?.let(::hasLabel) == true }
        fun hasNativeRole(node: AccessibilityNodeInfo): Boolean =
            node.className?.toString() in setOf("android.widget.Button", "android.widget.RadioButton")
        fun containsControl(node: AccessibilityNodeInfo): Boolean {
            // Compose의 Android 브리지는 이름과 역할을 자식으로 분리한다. 선택된 RadioButton은 클릭 불가다.
            val control = node.isClickable || node.isCheckable || hasNativeRole(node) ||
                (0 until node.childCount).any { node.getChild(it)?.let(::hasNativeRole) == true }
            if (node.isVisibleToUser && control && hasLabel(node)) return true
            return (0 until node.childCount).any { index -> node.getChild(index)?.let(::containsControl) == true }
        }
        fun describe(node: AccessibilityNodeInfo, depth: Int = 0): String = buildString {
            appendLine("${" ".repeat(depth)}${node.className} text=${node.text} desc=${node.contentDescription} visible=${node.isVisibleToUser} clickable=${node.isClickable}")
            repeat(node.childCount) { index -> node.getChild(index)?.let { append(describe(it, depth + 1)) } }
        }
        var actual = containsControl(root)
        val deadline = android.os.SystemClock.uptimeMillis() + 5_000
        while (actual != expected && android.os.SystemClock.uptimeMillis() < deadline) {
            Thread.sleep(100)
            root = checkNotNull(automation.rootInActiveWindow)
            actual = containsControl(root)
        }
        assertEquals("실제 Android 접근성 컨트롤: $label\n${if (actual != expected) describe(root) else ""}", expected, actual)
    }

    @Test
    fun partiallyCoveredFiltersAndBackRestoreTheirAccessibilityActions() {
        launch()
        revealQuestionControl("question_reference_open")
        compose.onNodeWithTag("question_reference_open").performClick()
        val list = compose.onNodeWithTag("book_detail_list")
        list.performScrollToNode(hasTestTag("review_scope_all"))
        val all = compose.onNodeWithTag("review_scope_all").performScrollTo()
        repeat(10) {
            val bounds = all.fetchSemanticsNode().boundsInRoot
            val panel = compose.onNodeWithTag("question_reference_overlay").fetchSemanticsNode().boundsInRoot
            val delta = bounds.top - (panel.bottom - bounds.height / 2)
            if (!(bounds.top < panel.bottom && bounds.bottom > panel.bottom)) list.performTouchInput {
                val shift = delta.coerceIn(-120f, 120f)
                swipe(start = androidx.compose.ui.geometry.Offset(4f, centerY),
                    end = androidx.compose.ui.geometry.Offset(4f, centerY - shift), durationMillis = 500)
            }
        }
        val hidden = SemanticsMatcher.keyIsDefined(SemanticsProperties.HideFromAccessibility)
        val bounds = all.fetchSemanticsNode().boundsInRoot
        val panel = compose.onNodeWithTag("question_reference_overlay").fetchSemanticsNode().boundsInRoot
        assertTrue("필터가 패널 경계에 걸린 상태여야 한다", bounds.top < panel.bottom && bounds.bottom > panel.bottom)
        all.assert(hidden)
        assertNativeAccessibleControl("전체", false)
        compose.onNodeWithTag("question_reference_close").performClick()
        all.assert(hidden.not())
        assertNativeAccessibleControl("전체", true)
        compose.onNodeWithTag("review_scope_mine").performTouchInput { click() }.assertIsSelected()
        all.performTouchInput { click() }.assertIsSelected()
        revealQuestionControl("question_reference_open")
        compose.onNodeWithTag("question_reference_open").performClick()
        list.performScrollToIndex(0)
        compose.onNodeWithTag("book_detail_back").assert(hidden)
        assertNativeAccessibleControl("뒤로 가기", false)
        compose.onNodeWithTag("question_reference_close").performClick()
        compose.onNodeWithTag("book_detail_back").assert(hidden.not()).assertHasClickAction()
        assertNativeAccessibleControl("뒤로 가기", true)
    }

    /** 테스트 Activity를 직접 회전해 작은 가로 화면을 검증한다. */
    @Test
    fun landscapeReferenceKeepsReadableBodyAndBackgroundScroll() {
        launch(landscape = true)
        revealQuestionControl("question_reference_open")
        compose.onNodeWithTag("question_reference_open").performClick()
        val list = compose.onNodeWithTag("book_detail_list")
        val viewport = list.fetchSemanticsNode().boundsInRoot
        assertTrue("가로 회전 화면에서 실행해야 한다", viewport.width > viewport.height)
        val panel = compose.onNodeWithTag("question_reference_overlay").fetchSemanticsNode().boundsInRoot
        val body = compose.onNodeWithTag("question_reference_body").fetchSemanticsNode().boundsInRoot
        val minimum = with(compose.density) { 44.dp.toPx() }
        assertTrue("본문을 직접 스크롤할 높이가 있어야 한다", body.height >= minimum - 1f)
        val belowPanel = viewport.bottom - panel.bottom >= minimum
        assertTrue("패널 밖 배경 목록 스크롤 영역이 있어야 한다", belowPanel || viewport.right - panel.right >= minimum)
        val before = scrollValue("book_detail_list")
        compose.onNodeWithTag("question_tab_Excerpt").performTouchInput { click() }.assertIsSelected()
        compose.onNodeWithTag("question_reference_body").performTouchInput { swipeUp() }
        assertTrue(scrollValue("question_reference_body") > 0)
        assertEquals(before, scrollValue("book_detail_list"), 0.01f)
        list.performTouchInput {
            val x = if (belowPanel) centerX else width - minimum / 2
            val endY = if (belowPanel) panel.bottom - viewport.top + 4f else 8f
            swipe(start = androidx.compose.ui.geometry.Offset(x, height - 4f),
                end = androidx.compose.ui.geometry.Offset(x, endY), durationMillis = 600)
        }
        assertTrue(scrollValue("book_detail_list") > before)
        compose.onNodeWithTag("question_reference_close").performTouchInput { click() }
        compose.onNodeWithTag("question_reference_overlay").assertDoesNotExist()
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
