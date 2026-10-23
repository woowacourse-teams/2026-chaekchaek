package com.chamsae.chaekchaek

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import android.view.KeyEvent
import android.view.MotionEvent
import android.os.SystemClock
import org.json.JSONObject
import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.data.remote.ApiConfiguration
import com.chaekchaek.app.ui.App
import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** public-library-fixture.py를 실행한 local 빌드의 실제 App/Navigation/API 경로를 검증한다. */
@RunWith(AndroidJUnit4::class)
class PublicLibraryNavigationTest {
    @get:Rule val compose = createComposeRule()
    private val base = "http://10.0.2.2:19087"
    private fun start(tab: String = "피드") {
        assertEquals(base, ApiConfiguration.current.baseUrl)
        check(URI("$base/__607/reset").toURL().readText().contains("607"))
        compose.setContent { App(AuthPlatformCallbacks(
            requestGoogleIdToken = { it(null, null) },
            readRefreshToken = { null }, writeRefreshToken = {}, clearRefreshToken = {},
        )) }
        waitFor("피드")
        compose.onNodeWithText(tab).performClick()
        if (tab == "피드") waitFor("독자A")
    }
    private fun waitFor(text: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun profile(name: String) {
        compose.onAllNodesWithContentDescription("$name 프로필").onFirst().performClick()
    }
    private fun back() { compose.onNodeWithContentDescription("뒤로 가기").performClick() }
    private fun feedItem(index: Int) {
        compose.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(index)
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val image = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            val file = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "$name-607.png")
            file.outputStream().use { check(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            image.recycle()
        }
    }
    @Test fun guestViewerKeepsFilterAndReturnsToExistingOwnerLibrary() {
        start(); profile("독자A"); waitFor("기본책장")
        capture("public-library")
        compose.onNodeWithText("기본책장").performClick(); waitFor("읽는 중")
        compose.onNodeWithText("읽는 중").performClick()
        capture("public-bookshelf")
        compose.onNodeWithText("서재 도서 1").performScrollTo().performClick()
        waitFor("공개 서재를 확인하는 감상")
        back(); waitFor("읽는 중")
        compose.onNodeWithText("읽는 중").assertIsSelected()
        compose.onNodeWithText("서재 도서 1").assertIsDisplayed()
        compose.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(9)
        compose.onNodeWithText("서재 도서 17").assertIsDisplayed().performClick()
        waitFor("공개 서재를 확인하는 감상")
        back(); waitFor("서재 도서 17")
        compose.onNodeWithText("서재 도서 17").assertIsDisplayed()
        compose.onNodeWithText("서재 도서 18").assertDoesNotExist()
        compose.onNodeWithText("서재 도서 17").performClick()
        waitFor("공개 서재를 확인하는 감상")
        compose.onAllNodesWithContentDescription("독자A 프로필").onFirst().performScrollTo().performClick()
        waitFor("기본책장")
        compose.onNodeWithText("읽는 중").assertDoesNotExist()
        back(); waitFor("피드")
        compose.onNodeWithText("독자A").assertIsDisplayed()
    }
    @Test fun emptyAndUnavailableAuthorsAndExplicitRetry() {
        start(); feedItem(1); profile("빈서재독자"); waitFor("아직 담은 책이 없어요"); back()
        feedItem(2); profile("오류서재독자"); waitFor("서재를 불러오지 못했어요")
        compose.onNodeWithText("다시 시도").performClick(); waitFor("기본책장"); back()
        listOf(3 to "익명으로 작성한 사용자의 서재는 볼 수 없어요.",
            4 to "비회원으로 작성한 사용자는 서재가 없어요.",
            5 to "탈퇴한 사용자의 서재는 볼 수 없어요.").forEach { (index, message) ->
            feedItem(index)
            profile(listOf("익명독자", "비회원독자", "탈퇴한 사용자")[index - 3])
            waitFor(message); compose.onNodeWithText("확인").performClick()
            compose.onNodeWithText("피드").assertIsDisplayed()
        }
    }
    @Test fun sameBookFromAnotherOwnerDoesNotShareDetailSort() {
        start(); profile("독자A"); waitFor("기본책장")
        compose.onNodeWithText("기본책장").performClick(); waitFor("읽는 중")
        compose.onNodeWithText("서재 도서 1").performScrollTo().performClick()
        waitFor("공개 서재를 확인하는 감상")
        compose.onNodeWithText("최신순").performScrollTo().performClick()
        compose.onNodeWithText("페이지순").performClick()
        compose.onAllNodesWithContentDescription("독자B 프로필").onFirst().performScrollTo().performClick()
        waitFor("B책장"); compose.onNodeWithText("B책장").performClick(); waitFor("읽는 중")
        compose.onNodeWithText("서재 도서 1").performScrollTo().performClick()
        waitFor("공개 서재를 확인하는 감상")
        compose.onNodeWithText("최신순").performScrollTo().assertIsDisplayed()
        back(); waitFor("읽는 중"); back(); waitFor("B책장"); back()
        waitFor("공개 서재를 확인하는 감상")
        compose.onNodeWithText("페이지순").performScrollTo().assertIsDisplayed()
    }

    private fun systemBack() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }
    private fun cancelSystemBackGesture() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val metrics = instrumentation.targetContext.resources.displayMetrics
        val down = SystemClock.uptimeMillis()
        fun input(action: Int, x: Float) {
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, metrics.heightPixels * 0.5f, 0)
            try { check(instrumentation.uiAutomation.injectInputEvent(event, true)) } finally { event.recycle() }
        }
        input(MotionEvent.ACTION_DOWN, 1f)
        // 시스템 가장자리에서 시작해 진행한 뒤 반대로 되돌려 취소한다.
        for (step in 1..20) { SystemClock.sleep(16); input(MotionEvent.ACTION_MOVE, metrics.widthPixels * 0.2f * step / 20) }
        for (step in 19 downTo 0) { SystemClock.sleep(16); input(MotionEvent.ACTION_MOVE, maxOf(1f, metrics.widthPixels * 0.2f * step / 20)) }
        input(MotionEvent.ACTION_UP, 1f)
        compose.waitForIdle()
    }
    private fun spineRow() = compose.onAllNodes(
        hasScrollToIndexAction() and hasAnyDescendant(hasContentDescription("서재 도서 1 책등")),
        useUnmergedTree = true,
    ).onLast()
    private fun spine17() = compose.onNodeWithContentDescription("서재 도서 17 책등", useUnmergedTree = true)

    @Test fun homeAndDiscoverProfilesReturnToTheirOriginalTabAndPosition() {
        start("홈")
        // 안쪽 가로 LazyRow만 스크롤하면 프로필이 화면 아래에 남는다.
        waitFor("독자A")
        compose.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(1)
        compose.onAllNodesWithContentDescription("독자A 프로필").onFirst().performScrollTo()
        val homeY = compose.onAllNodesWithContentDescription("독자A 프로필").onFirst().fetchSemanticsNode().boundsInRoot.top
        capture("home-profile-before")
        profile("독자A"); capture("home-profile-after"); waitFor("기본책장"); back()
        compose.onNodeWithText("홈").assertIsSelected()
        assertEquals(homeY, compose.onAllNodesWithContentDescription("독자A 프로필").onFirst().fetchSemanticsNode().boundsInRoot.top, 1f)
        compose.onNodeWithText("발견").performClick()
        compose.onAllNodesWithContentDescription("독자A 프로필").onFirst().performScrollTo()
        val discoverY = compose.onAllNodesWithContentDescription("독자A 프로필").onFirst().fetchSemanticsNode().boundsInRoot.top
        profile("독자A"); waitFor("기본책장"); systemBack()
        compose.onNodeWithText("발견").assertIsSelected()
        assertEquals(discoverY, compose.onAllNodesWithContentDescription("독자A 프로필").onFirst().fetchSemanticsNode().boundsInRoot.top, 1f)
        capture("home-discover-round-trip")
    }

    @Test fun replyProfileReusesExistingLibraryAndMixedBackKeepsRoot() {
        start(); profile("독자A"); waitFor("기본책장")
        compose.onNodeWithText("기본책장").performClick(); waitFor("읽는 중")
        compose.onNodeWithText("서재 도서 1").performScrollTo().performClick()
        waitFor("공개 서재를 확인하는 감상")
        compose.onAllNodesWithContentDescription("답글독자 프로필").onFirst().performScrollTo().performTouchInput { doubleClick() }
        waitFor("기본책장")
        // 답글 작성자도 회원 42다. 새 서재를 쌓지 않고 기존 A 서재까지 복귀한다.
        compose.onNodeWithText("독자A").assertIsDisplayed()
        systemBack(); waitFor("피드")
        compose.onNodeWithText("피드").assertIsSelected()
        // 재진입 뒤 화면 버튼 연타와 시스템 뒤로가기를 섞어도 루트가 남는다.
        profile("독자A"); waitFor("기본책장")
        compose.onNodeWithText("기본책장").performTouchInput { doubleClick() }; waitFor("읽는 중")
        cancelSystemBackGesture()
        compose.onNodeWithText("읽는 중").assertIsDisplayed()
        compose.onNodeWithContentDescription("뒤로 가기").performTouchInput { doubleClick() }
        waitFor("기본책장"); systemBack(); waitFor("피드")
        compose.onNodeWithText("발견").performClick(); compose.onNodeWithText("발견").assertIsSelected()
        capture("reply-mixed-back-root")
    }

    @Test fun horizontalSpinesEmptyBookshelfAndErrorClosePreserveNavigation() {
        start(); profile("독자A"); waitFor("기본책장")
        spineRow().performScrollToIndex(16)
        val libraryX = spine17().assertIsDisplayed().fetchSemanticsNode().boundsInRoot.left
        compose.onNodeWithText("기본책장").performClick(); waitFor("읽는 중")
        spineRow().performScrollToIndex(16)
        val shelfX = spine17().assertIsDisplayed().fetchSemanticsNode().boundsInRoot.left
        spine17().performClick(); waitFor("공개 서재를 확인하는 감상"); back()
        assertEquals(shelfX, spine17().assertIsDisplayed().fetchSemanticsNode().boundsInRoot.left, 1f)
        back(); waitFor("기본책장")
        assertEquals(libraryX, spine17().assertIsDisplayed().fetchSemanticsNode().boundsInRoot.left, 1f)
        compose.onNodeWithText("빈책장").performScrollTo().performClick()
        waitFor("전체"); compose.onNodeWithText("아직 담은 책이 없어요").assertIsDisplayed()
        back(); waitFor("기본책장"); back(); feedItem(2); profile("오류서재독자")
        waitFor("서재를 불러오지 못했어요"); compose.onNodeWithText("닫기").performClick()
        compose.onNodeWithText("서재를 불러오지 못했어요").assertDoesNotExist()
        compose.onNodeWithText("오류서재독자").assertIsDisplayed()
        assertEquals(1, JSONObject(URI("$base/__607").toURL().readText()).getJSONObject("counts").getInt("/api/v1/libraries?memberId=44"))
        systemBack(); waitFor("피드")
        compose.onNodeWithText("오류서재독자").assertIsDisplayed()
        profile("오류서재독자"); waitFor("기본책장")
        assertEquals(2, JSONObject(URI("$base/__607").toURL().readText()).getJSONObject("counts").getInt("/api/v1/libraries?memberId=44"))
        capture("horizontal-empty-error-close")
    }
}
