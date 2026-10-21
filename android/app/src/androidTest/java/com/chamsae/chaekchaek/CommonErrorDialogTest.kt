package com.chamsae.chaekchaek

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chaekchaek.app.presentation.common.*
import com.chaekchaek.app.ui.common.ChaekErrorDialog
import com.chaekchaek.app.ui.common.ErrorRecoveryEffect
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import org.junit.Assert.*
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommonErrorDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun retryButtonsAreHorizontalAndClosingKeepsPendingInput() {
        val errors = ErrorRecoveryState()
        val draft = mutableStateOf("보존할 감상 초안")
        var calls = 0
        errors.report(Exception(), ErrorOperation.LibrarySave) { calls++ }
        compose.setContent {
            ChaekchaekTheme {
                androidx.compose.material3.Text(draft.value)
                ErrorRecoveryEffect(errors)
            }
        }
        val close = compose.onNodeWithText("닫기")
        val retry = compose.onNodeWithText("다시 시도")
        val closeBounds = close.fetchSemanticsNode().boundsInRoot
        val retryBounds = retry.fetchSemanticsNode().boundsInRoot
        assertEquals(closeBounds.center.y, retryBounds.center.y, 1f)
        assertTrue(closeBounds.right < retryBounds.left)
        capture("common-error-retry")
        close.performClick()
        compose.onNodeWithText("보존할 감상 초안").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(0, calls)
            errors.report(Exception(), ErrorOperation.LibrarySave) { calls++ }
        }
        compose.onNodeWithText("다시 시도").performClick()
        compose.runOnIdle { assertEquals(1, calls) }
        compose.onNodeWithText("다시 시도").assertDoesNotExist()
    }

    @Test
    fun loginVariantUsesSameHorizontalActionsAndSeparateTopCloseRow() {
        val visible = mutableStateOf(true)
        compose.setContent {
            ChaekchaekTheme {
                if (visible.value) ChaekErrorDialog(
                    ErrorNotice(1, "다시 로그인해 주세요", "로그인한 뒤 계속할 수 있어요.", ErrorAction.Login),
                    { visible.value = false }, {},
                )
            }
        }
        val title = compose.onNodeWithText("다시 로그인해 주세요").fetchSemanticsNode().boundsInRoot
        val close = compose.onNodeWithContentDescription("닫기")
        val closeBounds = close.fetchSemanticsNode().boundsInRoot
        assertTrue(closeBounds.bottom <= title.top)
        assertTrue(closeBounds.right > title.right)
        val login = compose.onNodeWithText("로그인").fetchSemanticsNode().boundsInRoot
        val dismiss = compose.onNodeWithText("닫기").fetchSemanticsNode().boundsInRoot
        assertEquals(login.center.y, dismiss.center.y, 1f)
        capture("common-error-login")
        close.performClick()
        compose.onNodeWithText("로그인").assertDoesNotExist()
    }

    @Test
    fun confirmHasOneActionAndDoesNotExecuteRetry() {
        val errors = ErrorRecoveryState()
        errors.confirm("쪽수는 0쪽부터 224쪽까지 입력해 주세요.")
        compose.setContent { ChaekchaekTheme { ErrorRecoveryEffect(errors) } }
        compose.onNodeWithText("다시 시도").assertDoesNotExist()
        compose.onNodeWithText("닫기").assertDoesNotExist()
        capture("common-error-confirm")
        compose.onNodeWithText("확인").performClick()
        compose.runOnIdle { assertNull(errors.notice.value) }
    }
    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(400)
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png").outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        screenshot.recycle()
    }
}
