package com.chamsae.chaekchaek

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.data.remote.ApiConfiguration
import com.chaekchaek.app.ui.App
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI

/** 최신 책장 계약의 합성 서버로 실제 공유 UI와 저장 호출을 검증한다. */
@RunWith(AndroidJUnit4::class)
class BookshelfContractTest {
    @get:Rule val compose = createComposeRule()
    private val origin = "http://10.0.2.2:19068"

    @Test fun createRenameAddRemoveAndImagePreview() {
        Assume.assumeTrue("bookshelf-contract local fixture가 필요합니다", ApiConfiguration.current.baseUrl == origin)
        request("/__bookshelf/reset", "POST")
        compose.setContent { App(AuthPlatformCallbacks({ it(null, null) }, { "fixture-refresh" }, {}, {})) }
        waitText("내 서재")
        compose.onNodeWithText("내 서재").performClick()
        waitTag("bookshelf-1")
        compose.onNodeWithTag("bookshelf-menu-1").performClick()
        compose.onNodeWithText("이름 바꾸기").performClick()
        compose.onNodeWithTag("bookshelfName").performTextReplacement("나의 책장")
        compose.onNodeWithText("저장").performClick()
        waitText("나의 책장")
        compose.onNodeWithTag("bookshelf-create").performClick()
        compose.onNodeWithTag("bookshelfName").performTextInput("올해읽을책")
        compose.onNodeWithText("만들기").performClick()
        waitTag("bookshelf-1")
        compose.onNodeWithTag("bookshelf-1").performClick()
        waitText("+ 책 담기")
        compose.onNodeWithText("+ 책 담기").performScrollTo().performClick()
        compose.onNodeWithText("검증 도서 3").performScrollTo().performClick()
        waitText("담을 책장 고르기")
        compose.onNodeWithText("이 책장에 담기").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("담을 책장 고르기").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("편집").performScrollTo().performClick()
        compose.onAllNodesWithText("빼기").onFirst().performScrollTo().performClick()
        compose.onNodeWithText("완료").performScrollTo().performClick()
        compose.onNodeWithText("이미지 공유").performScrollTo().performClick()
        waitText("책장을 이미지로")
        compose.onNodeWithText("이미지 저장").performScrollTo().assertIsDisplayed()
        val state = JSONObject(request("/__bookshelf/state"))
        val shelves = state.getJSONArray("shelves")
        assertTrue((0 until shelves.length()).any { shelves.getJSONObject(it).getString("name") == "올해읽을책" })
        val requests = state.getJSONArray("requests")
        val calls = (0 until requests.length()).map { requests.getJSONObject(it) }
        assertTrue(calls.any { it.getString("method") == "POST" && it.getString("path") == "/api/v1/libraries/1" })
        assertTrue(calls.any { it.getString("method") == "DELETE" && it.getString("path").contains("/books/") })
        compose.waitUntil(10_000) { JSONObject(request("/__bookshelf/state")).getJSONArray("requests").toString().contains("/api/v1/books/3/reviews") }
    }

    private fun waitText(text: String) = compose.waitUntil(15_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    private fun waitTag(tag: String) = compose.waitUntil(15_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun request(path: String, method: String = "GET"): String {
        val connection = URI(origin + path).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 2_000; connection.readTimeout = 2_000; connection.requestMethod = method
        return try { connection.inputStream.bufferedReader().use { it.readText() } } finally { connection.disconnect() }
    }
}
