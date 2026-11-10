package com.chamsae.chaekchaek

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.chaekchaek.app.data.remote.ApiConfiguration
import java.net.URI
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith

/** local 빌드 + book-deeplink-fixture.py. 기존 인증 데이터가 있으면 실행하지 않는다. */
@RunWith(AndroidJUnit4::class)
class BookDeepLinkInteractionTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val base get() = ApiConfiguration.current.baseUrl
    private var priorOnboarding: Boolean? = null
    private var prepared = false
    private val valid = "chaekchaek://books/9788937460449"

    @Before fun prepare() {
        assertEquals("local", ApiConfiguration.environment)
        Assume.assumeFalse("인증 데이터가 있는 기기는 보존한다", context.getSharedPreferences("auth_session", Context.MODE_PRIVATE).contains("refresh_token"))
        check(URI("$base/__692/reset").toURL().readText().contains("692"))
        val preferences = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
        priorOnboarding = if (preferences.contains("first_library_shown")) preferences.getBoolean("first_library_shown", false) else null
        prepared = true
        preferences.edit().putBoolean("first_library_shown", true).commit()
    }
    @After fun finish() {
        if (!prepared) return
        instrumentation.runOnMainSync {
            Stage.values().flatMap { ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it) }
                .filter { it is MainActivity || it is DeveloperToolsActivity }.distinct().forEach { it.finish() }
        }
        val edit = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE).edit()
        priorOnboarding?.let { edit.putBoolean("first_library_shown", it) } ?: edit.remove("first_library_shown")
        edit.commit()
    }
    private fun link(uri: String = valid) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage(context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    private fun waitText(text: String) = compose.waitUntil(15_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    private fun detail() { waitText("딥링크 검증 도서") }
    private fun back() { instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK); compose.waitForIdle() }
    private fun main() = instrumentation.runOnMainSync {
        val activity = Stage.values().flatMap { ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it) }.filterIsInstance<MainActivity>().first()
        activity.recreate()
    }
    private fun counts() = JSONObject(URI("$base/__692").toURL().readText()).getJSONObject("counts")
    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            java.io.File(context.getExternalFilesDir(null), "692-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { bitmap.recycle() }
    }

    @Test fun coldThenWarmAndBackgroundLinksPreserveDiscoverInput() {
        link(); detail(); capture("detail"); back(); waitText("홈")
        compose.onNodeWithText("발견").performClick()
        compose.onNodeWithTag("search-query-field").performTextReplacement("보존할 검색어")
        link(); detail(); back()
        compose.onNodeWithTag("search-query-field").assertTextEquals("보존할 검색어")
        // 다른 Activity를 열어 앱을 백그라운드로 보낸 뒤 같은 암시적 URI를 수신한다.
        val pfd = instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_HOME")
        java.io.FileInputStream(pfd.fileDescriptor).use { it.readBytes() }
        pfd.close()
        Thread.sleep(1000)
        link(); detail(); back()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("search-query-field").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("search-query-field").assertTextEquals("보존할 검색어")
    }

    @Test fun sameBookPreservesScrollAndSortWithoutAnotherLookup() {
        link(); detail()
        compose.onNodeWithText("인기순").performScrollTo().performClick()
        compose.onNodeWithText("최신순").performClick()
        compose.onNodeWithTag("book_detail_list").performScrollToIndex(2)
        val before = compose.onNodeWithTag("book_detail_list").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        val requests = counts().getInt("/api/v1/books/by-isbn/9788937460449")
        link(); compose.waitForIdle()
        compose.onNodeWithText("최신순").assertExists()
        assertEquals(before, compose.onNodeWithTag("book_detail_list").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value())
        assertEquals(requests, counts().getInt("/api/v1/books/by-isbn/9788937460449"))
        back(); waitText("홈")
    }

    @Test fun invalidMissingAndRetryKeepPreviousScreen() {
        link("chaekchaek://books/not-a-book"); waitText("링크를 열 수 없어요")
        compose.onNodeWithText("확인").performClick(); waitText("홈")
        compose.onNodeWithText("발견").performClick()
        compose.onNodeWithTag("search-query-field").performTextReplacement("이전 입력")
        link("chaekchaek://books/9791172376215"); waitText("책을 찾을 수 없어요")
        capture("missing"); compose.onNodeWithText("확인").performClick()
        compose.onNodeWithTag("search-query-field").assertTextEquals("이전 입력")
        link("chaekchaek://books/9788936433598"); waitText("다시 시도")
        compose.onNodeWithText("다시 로그인해 주세요").assertDoesNotExist()
        compose.onNodeWithText("다시 시도").performClick(); detail(); back()
        compose.onNodeWithTag("search-query-field").assertTextEquals("이전 입력")
    }

    @Test fun recreationDoesNotReopenConsumedIntentButNewSameUriDoes() {
        link(); detail(); back(); waitText("홈")
        main(); waitText("홈")
        compose.onNodeWithText("딥링크 검증 도서").assertDoesNotExist()
        link(); detail()
        main(); detail(); back(); waitText("홈")
    }

    @Test fun onboardingDefersTargetUntilSkipped() {
        context.getSharedPreferences("onboarding", Context.MODE_PRIVATE).edit().putBoolean("first_library_shown", false).commit()
        link(); waitText("건너뛰기")
        compose.onNodeWithText("딥링크 검증 도서").assertDoesNotExist()
        main(); waitText("건너뛰기")
        compose.onNodeWithText("건너뛰기").performClick(); detail(); back(); waitText("홈")
    }

    @Test fun developerToolKeepsInvalidInputAndOpensRealUri() {
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        waitText("DEV"); compose.onNodeWithText("DEV").performClick(); waitText("책 상세 딥링크")
        capture("developer-tools")
        compose.onNodeWithText("잘못된 링크").performClick(); compose.onNodeWithText("링크 실행").performClick()
        waitText("링크를 열 수 없어요"); compose.onNodeWithText("확인").performClick()
        compose.onNodeWithTag("developer-deeplink-uri").assertTextEquals("chaekchaek://books/not-a-book")
        compose.onNodeWithText("정상 링크").performClick(); compose.onNodeWithText("링크 실행").performClick()
        detail(); back(); waitText("홈")
    }
}
