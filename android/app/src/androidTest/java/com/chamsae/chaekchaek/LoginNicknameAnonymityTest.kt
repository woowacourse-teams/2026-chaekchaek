package com.chamsae.chaekchaek

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.data.remote.ApiConfiguration
import com.chaekchaek.app.ui.App
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import com.chaekchaek.app.ui.bookdetail.BookDetailScreen
import com.chaekchaek.app.ui.bookdetail.BookDetailUiState
import com.chaekchaek.app.ui.bookdetail.QuestionAReviewDraft
import com.chaekchaek.app.ui.bookdetail.ReviewSubmissionState
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 전용 로컬 HTTP fixture와 local API 빌드에서 실행한다. 실제 OAuth는 수행하지 않는다. */
@RunWith(AndroidJUnit4::class)
class LoginNicknameAnonymityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun newMemberMustSaveNicknameBeforeLoginCompletes() = nicknameFlow(restored = false)

    @Test fun restoredMemberWithoutNicknameMustSaveAndCanChangeItLater() = nicknameFlow(restored = true)

    private fun nicknameFlow(restored: Boolean) {
        val base = "http://10.0.2.2:19080"
        val marker = runCatching { URI("$base/__580").toURL().readText() }.getOrDefault("")
        assumeTrue("#580 전용 로컬 fixture가 필요하다", marker == "580")
        assertEquals(base, ApiConfiguration.current.baseUrl)
        val reset = URI("$base/__580/reset").toURL().openConnection() as HttpURLConnection
        reset.requestMethod = "POST"
        assertEquals(200, reset.responseCode)
        reset.disconnect()
        var refresh: String? = if (restored) "fixture-refresh" else null
        val callbacks = AuthPlatformCallbacks(
            requestGoogleIdToken = { it("fixture-google", null) },
            readRefreshToken = { refresh },
            writeRefreshToken = { refresh = it },
            clearRefreshToken = { refresh = null },
        )
        compose.setContent { App(callbacks) }
        if (!restored) {
            compose.onNodeWithText("내 서재").performClick()
            compose.onNodeWithText("편집").performClick()
            compose.onNodeWithText("Google로 계속하기").assertIsDisplayed()
            capture("google-login")
            compose.onNodeWithText("Google로 계속하기").performClick()
        }
        waitFor("닉네임 설정")
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("닉네임 설정").assertIsDisplayed()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val time = SystemClock.uptimeMillis()
        listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
            MotionEvent.obtain(time, SystemClock.uptimeMillis(), action, 10f, 100f, 0).also {
                instrumentation.sendPointerSync(it)
                it.recycle()
            }
        }
        compose.onNodeWithText("닉네임 설정").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextInput("reader")
        compose.onNodeWithText("확인").performClick()
        waitFor("설정을 변경하지 못했어요")
        compose.onNodeWithText("닫기").performClick()
        compose.onNode(hasSetTextAction()).assertTextEquals("reader")
        capture(if (restored) "restored-nickname-retained" else "new-nickname-retained")
        compose.onNodeWithText("확인").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("닉네임 설정").fetchSemanticsNodes().isEmpty() }
        var server = JSONObject(URI("$base/__580/state").toURL().readText())
        assertEquals(2, server.getInt("nicknameWrites"))
        assertEquals("reader", server.getString("nickname"))
        if (restored) {
            compose.onNodeWithContentDescription("내 프로필").performClick()
            waitFor("닉네임 변경")
            compose.onNodeWithText("닉네임 변경").performClick()
            compose.onNode(hasSetTextAction()).performTextReplacement("changed")
            compose.onNodeWithText("확인").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("닉네임 설정").fetchSemanticsNodes().isEmpty() }
            server = JSONObject(URI("$base/__580/state").toURL().readText())
            assertEquals("changed", server.getString("nickname"))
            assertTrue(server.getBoolean("displayAnonymous"))
        }
    }

    @Test fun anonymousChoicePersistsLocallyAndSurvivesFailedSubmissionAndRestoration() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("qa_580_review_preferences", 0)
        preferences.edit().putBoolean("anonymous", true).commit()
        val state = mutableStateOf(BookDetailUiState(
            book = BookDetailArgs(id = "42", bookId = 42, title = "익명 선택 검증", totalPages = 224),
            signedIn = true,
        ))
        val submitted = mutableListOf<QuestionAReviewDraft>()
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            ChaekchaekTheme {
                BookDetailScreen(
                    state = state.value, onBack = {},
                    anonymousReviews = preferences.getBoolean("anonymous", true),
                    onAnonymousReviewsChange = { preferences.edit().putBoolean("anonymous", it).commit() },
                    onQuestionAReviewCreate = {
                        submitted += it
                        state.value = state.value.copy(reviewSubmissionState = ReviewSubmissionState.Submitting)
                    },
                    onReviewOpen = { ready -> ready() },
                    onRequestErrorShown = { state.value = state.value.copy(requestError = null) },
                )
            }
        }
        compose.onNodeWithText("이 순간의 감상 남기기", substring = true).performClick()
        compose.onNodeWithContentDescription("감상 (필수)").performTextInput("실패해도 보존할 감상")
        compose.onNodeWithTag("reviewAnonymousCheckbox").performScrollTo().assertIsOn().performClick().assertIsOff()
        assertFalse(preferences.getBoolean("anonymous", true))
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("reviewAnonymousCheckbox").assertIsOff()
        compose.onAllNodes(hasText("감상 남기기") and hasClickAction()).onLast().performScrollTo().performClick()
        compose.onNode(hasText("감상 남기기") and hasClickAction()).assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(1, submitted.size)
            assertEquals(false, submitted.single().anonymous)
            state.value = state.value.copy(reviewSubmissionState = ReviewSubmissionState.Failed,
                requestError = "요청을 처리하지 못했어요. 다시 시도해 주세요.")
        }
        compose.onNodeWithText("닫기").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("감상 (필수)").assertTextEquals("실패해도 보존할 감상")
        compose.onNodeWithTag("reviewAnonymousCheckbox").performScrollTo().assertIsOff()
        capture("anonymous-failure-retained")
        compose.onNodeWithTag("reviewAnonymousCheckbox").performClick().assertIsOn()
        assertTrue(preferences.getBoolean("anonymous", false))
    }

    private fun waitFor(text: String) = compose.waitUntil(10_000) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "580-$name.png").outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        screenshot.recycle()
    }
}
