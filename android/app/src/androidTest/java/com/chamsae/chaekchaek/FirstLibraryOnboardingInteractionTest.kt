package com.chamsae.chaekchaek

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performImeAction
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import android.view.KeyEvent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.domain.book.BookSearchResult
import com.chaekchaek.app.ui.App
import com.chaekchaek.app.ui.onboarding.FirstLibraryOnboardingScreen
import com.chaekchaek.app.ui.onboarding.FirstLibraryOnboardingState
import com.chaekchaek.app.ui.onboarding.OnboardingStep
import com.chaekchaek.app.ui.onboarding.OnboardingStorage
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirstLibraryOnboardingInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun systemBackAndHeaderReturnToThePreviousOnboardingStep() {
        compose.setContent {
            App(
                authPlatform = AuthPlatformCallbacks(
                    requestGoogleIdToken = { _ -> },
                    readRefreshToken = { null },
                    writeRefreshToken = {},
                    clearRefreshToken = {},
                ),
                onboardingStorage = remember { OnboardingStorage(hasBeenShown = { false }, markShown = {}) },
            )
        }
        compose.onNodeWithText("책 찾기").performClick()
        compose.onNodeWithText("어떤 책을 읽고 있나요?").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("내 서재의 첫 책을\n골라볼까요?").assertIsDisplayed()
        compose.onNodeWithText("책 찾기").performClick()
        compose.onNodeWithContentDescription("이전").performClick()
        compose.onNodeWithText("내 서재의 첫 책을\n골라볼까요?").assertIsDisplayed()
    }

    @Test
    @Suppress("DEPRECATION")
    fun searchLimitsRealEditsAndPreservesTheCursorAtThePasteBoundary() {
        var query by mutableStateOf("")
        lateinit var clipboard: ClipboardManager
        compose.setContent {
            val manager = LocalClipboardManager.current
            SideEffect { clipboard = manager }
            ChaekchaekTheme {
                FirstLibraryOnboardingScreen(
                    state = FirstLibraryOnboardingState(step = OnboardingStep.SEARCH, query = query),
                    onStartSearch = {}, onBack = {}, onQueryChange = { query = it }, onSearch = {},
                    onToggleBook = {}, onReviewSelection = {}, onLogin = {}, onRetrySave = {}, onSkip = {},
                )
            }
        }
        val field = compose.onNode(hasSetTextAction())
        field.performTextReplacement("가".repeat(99) + "📚초과")
        field.assertTextEquals("가".repeat(99) + "📚")
        val fullQuery = "가".repeat(50) + "나".repeat(50)
        field.performTextReplacement(fullQuery)
        field.performSemanticsAction(SemanticsActions.SetSelection) { it(50, 50, false) }
        compose.runOnIdle { clipboard.setText(AnnotatedString("📚".repeat(10_000))) }
        field.performSemanticsAction(SemanticsActions.PasteText) { it() }
        field.assertTextEquals(fullQuery)
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(50)))
    }

    @Test
    fun searchKeyboardDismissalAllowsRepeatedInputReentryAndPreservesQuery() {
        var query by mutableStateOf("")
        var searches = 0
        val book = BookSearchResult(title = "첫 책", creator = "작가", publisher = "출판사", year = "2026", coverUrl = "", isbn13 = "1", totalPages = 100)
        compose.setContent {
            ChaekchaekTheme {
                Box(Modifier.testTag("onboarding-keyboard-fixture")) {
                    FirstLibraryOnboardingScreen(
                        state = FirstLibraryOnboardingState(step = OnboardingStep.SEARCH, query = query, results = listOf(book)),
                        onStartSearch = {}, onBack = {}, onQueryChange = { query = it }, onSearch = { searches++ },
                        onToggleBook = {}, onReviewSelection = {}, onLogin = {}, onRetrySave = {}, onSkip = {},
                    )
                }
            }
        }
        val fixture = compose.onNodeWithTag("onboarding-keyboard-fixture")
        val field = compose.onNode(hasSetTextAction())
        field.performTouchInput { click() }.assertIsFocused()
        // 실제 탭 이후 네이티브 키 이벤트로 입력해 포커스와 입력 연결을 확인한다.
        InstrumentationRegistry.getInstrumentation().sendStringSync("keyboard")
        compose.waitForIdle()
        field.assertTextEquals("keyboard")
        repeat(3) {
            val fieldBounds = field.fetchSemanticsNode().boundsInRoot
            val guidanceBounds = compose.onNodeWithText("책 제목이나 작가로 찾아보세요.").fetchSemanticsNode().boundsInRoot
            val rootBounds = fixture.fetchSemanticsNode().boundsInRoot
            assertTrue("입력칸 위 빈 영역에 탭할 공간이 있어야 한다", fieldBounds.top > guidanceBounds.bottom)
            val blank = Offset(fieldBounds.center.x, (guidanceBounds.bottom + fieldBounds.top) / 2) - rootBounds.topLeft
            fixture.performTouchInput { click(blank) }
            field.assertIsNotFocused().assertTextEquals("keyboard")
            field.performTouchInput { click() }.assertIsFocused().assertTextEquals("keyboard")
        }
        field.performImeAction()
        field.assertIsNotFocused().assertTextEquals("keyboard")
        compose.runOnIdle {
            assertEquals("keyboard", query)
            assertEquals(1, searches)
        }
    }

    @Test
    fun bookRowExposesItsCheckboxRoleAndCheckedState() {
        val book = BookSearchResult(title = "첫 책", creator = "작가", publisher = "출판사", year = "2026", coverUrl = "", isbn13 = "1", totalPages = 100)
        compose.setContent {
            var selected by remember { mutableStateOf(false) }
            ChaekchaekTheme {
                FirstLibraryOnboardingScreen(
                    state = FirstLibraryOnboardingState(
                        step = OnboardingStep.SEARCH,
                        results = listOf(book),
                        selectedBooks = if (selected) listOf(book) else emptyList(),
                    ),
                    onStartSearch = {}, onBack = {}, onQueryChange = {}, onSearch = {},
                    onToggleBook = { selected = !selected }, onReviewSelection = {},
                    onLogin = {}, onRetrySave = {}, onSkip = {},
                )
            }
        }
        val row = compose.onNodeWithText("첫 책")
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).assertIsOff()
        row.performClick().assertIsOn()
        row.performClick().assertIsOff()
    }
}
