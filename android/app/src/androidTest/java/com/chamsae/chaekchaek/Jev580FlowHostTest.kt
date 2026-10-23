package com.chamsae.chaekchaek

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.data.remote.ApiConfiguration
import com.chaekchaek.app.ui.App
import com.chaekchaek.app.ui.bookdetail.*
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import java.net.HttpURLConnection
import java.net.URI
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Jev가 조작할 실제 공통 화면. OAuth와 저장 대상만 전용 로컬 fixture로 제한한다. */
@RunWith(AndroidJUnit4::class)
class Jev580FlowHostTest {
    @get:Rule val compose = createComposeRule()
    private val base = "http://10.0.2.2:19080"

    @Test fun exposeRealScreensForExternalJevActions() {
        assumeTrue(get("/__580") == "580")
        assertEquals(base, ApiConfiguration.current.baseUrl)
        val flow = InstrumentationRegistry.getArguments().getString("jevFlow") ?: error("jevFlow required")
        post("/__580/reset")
        if (flow == "review") review() else nickname(flow == "nickname-restored")
    }

    private fun nickname(restored: Boolean) {
        var refresh: String? = if (restored) "fixture-refresh" else null
        compose.setContent {
            App(AuthPlatformCallbacks(
                requestGoogleIdToken = { it("fixture-google", null) },
                readRefreshToken = { refresh }, writeRefreshToken = { refresh = it }, clearRefreshToken = { refresh = null },
            ))
        }
        if (!restored) {
            compose.onNodeWithText("내 서재").performClick()
            compose.onNodeWithText("편집").performClick()
            compose.onNodeWithText("Google로 계속하기").performClick()
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("닉네임 설정").fetchSemanticsNodes().isNotEmpty() }
        post("/__580/ready?phase=nickname")
        awaitFinish()
        val state = JSONObject(get("/__580/state"))
        assertEquals("reader", state.getString("nickname"))
        val expectedWrites = InstrumentationRegistry.getArguments().getString("nicknameWrites")?.toInt() ?: 2
        require(expectedWrites in 1..2)
        assertEquals(expectedWrites, state.getInt("nicknameWrites"))
        compose.onNodeWithText("닉네임 설정").assertDoesNotExist()
    }

    private fun review() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("qa_580_jev_preferences", 0)
        val hadAnonymous = prefs.contains("anonymous")
        val previousAnonymous = prefs.getBoolean("anonymous", true)
        prefs.edit().putBoolean("anonymous", true).commit()
        try {
            val anonymousReviews = mutableStateOf(prefs.getBoolean("anonymous", true))
            val state = mutableStateOf(BookDetailUiState(
                book = BookDetailArgs(id = "42", bookId = 42, title = "익명 선택 검증", totalPages = 224), signedIn = true,
            ))
            val submissions = mutableListOf<QuestionAReviewDraft>()
            val restoration = StateRestorationTester(compose)
            restoration.setContent {
                ChaekchaekTheme {
                    BookDetailScreen(state = state.value, onBack = {},
                        anonymousReviews = anonymousReviews.value,
                        onAnonymousReviewsChange = { prefs.edit().putBoolean("anonymous", it).commit(); anonymousReviews.value = it },
                        onReviewOpen = { it() },
                        onQuestionAReviewCreate = {
                            submissions += it
                            state.value = state.value.copy(reviewSubmissionState = ReviewSubmissionState.Failed,
                                requestError = "요청을 처리하지 못했어요. 다시 시도해 주세요.")
                        },
                        onRequestErrorShown = { state.value = state.value.copy(requestError = null) },
                    )
                }
            }
            compose.onNodeWithText("이 순간의 감상 남기기", substring = true).performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("reviewAnonymousSetting").fetchSemanticsNodes().isNotEmpty() }
            post("/__580/ready?phase=review")
            var restored = false
            val until = System.currentTimeMillis() + 600_000
            while (System.currentTimeMillis() < until) {
                when (get("/__580/control")) {
                    "restore" -> if (!restored) {
                        assertEquals(1, submissions.size)
                        assertEquals(false, submissions.single().anonymous)
                        assertEquals("draft", submissions.single().content)
                        assertFalse(prefs.getBoolean("anonymous", true))
                        restoration.emulateSavedInstanceStateRestore()
                        compose.onNodeWithContentDescription("감상 (필수)").assertTextEquals("draft")
                        compose.onNodeWithTag("reviewAnonymousSetting").assertIsOff()
                        restored = true
                        post("/__580/ready?phase=review-restored")
                    }
                    "finish" -> {
                        assertTrue(restored)
                        assertTrue(prefs.getBoolean("anonymous", false))
                        compose.onNodeWithTag("reviewAnonymousSetting").assertIsOn()
                        return
                    }
                }
                compose.waitForIdle()
                Thread.sleep(200)
            }
            error("Jev 제어 대기 시간 초과")
        } finally {
            val edit = prefs.edit()
            if (hadAnonymous) edit.putBoolean("anonymous", previousAnonymous) else edit.remove("anonymous")
            edit.commit()
        }
    }

    private fun awaitFinish() {
        val until = System.currentTimeMillis() + 600_000
        while (System.currentTimeMillis() < until) {
            if (get("/__580/control") == "finish") return
            compose.waitForIdle()
                Thread.sleep(200)
        }
        error("Jev 제어 대기 시간 초과")
    }

    private fun get(path: String) = URI("$base$path").toURL().readText()
    private fun post(path: String) {
        val connection = URI("$base$path").toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        assertEquals(200, connection.responseCode)
        connection.disconnect()
    }
}
