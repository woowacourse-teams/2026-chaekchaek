package com.chamsae.chaekchaek

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
    private fun start() {
        assertEquals(base, ApiConfiguration.current.baseUrl)
        check(URI("$base/__607/reset").toURL().readText().contains("607"))
        compose.setContent { App(AuthPlatformCallbacks(
            requestGoogleIdToken = { it(null, null) },
            readRefreshToken = { null }, writeRefreshToken = {}, clearRefreshToken = {},
        )) }
        waitFor("피드")
        compose.onNodeWithText("피드").performClick()
        waitFor("독자A")
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
    @Test fun guestViewerKeepsFilterAndReturnsToExistingOwnerLibrary() {
        start(); profile("독자A"); waitFor("기본책장")
        compose.onNodeWithText("기본책장").performClick(); waitFor("읽는 중")
        compose.onNodeWithText("읽는 중").performClick()
        compose.onNodeWithText("서재 도서 1").performScrollTo().performClick()
        waitFor("공개 서재를 확인하는 감상")
        back(); waitFor("읽는 중")
        compose.onNodeWithText("읽는 중").assertIsSelected()
        compose.onNodeWithText("서재 도서 1").assertIsDisplayed()
        compose.onNodeWithText("서재 도서 1").performClick()
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
}
