package com.chamsae.chaekchaek

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.data.remote.ApiConfiguration
import com.chaekchaek.app.ui.App
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

/** scripts/reply-compatibility-fixture.py와 -PapiEnvironment=local로 실행한다. */
@RunWith(AndroidJUnit4::class)
class ReplyResponseCompatibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun missingDeletedSupportsReplyRoundTripAndReentry() {
        val marker = runCatching {
            val connection = URI("http://10.0.2.2:19063/__reply-compat").toURL().openConnection()
            connection.connectTimeout = 1_000
            connection.readTimeout = 1_000
            connection.getInputStream().bufferedReader().use { it.readText() }
        }.getOrDefault("")
        assumeTrue("reply-compat fixture를 실행한 local 빌드에서 검증한다", marker.contains("reply-compat"))
        assertEquals("http://10.0.2.2:19063", ApiConfiguration.current.baseUrl)
        val reset = URI("http://10.0.2.2:19063/__reply-compat/reset").toURL().openConnection() as HttpURLConnection
        reset.requestMethod = "POST"
        assertEquals(200, reset.responseCode)
        reset.disconnect()
        compose.setContent { App(AuthPlatformCallbacks({ it(null, null) }, { null }, {}, {})) }
        waitForText("Reply compatibility")
        compose.onNodeWithText("Reply compatibility").performClick()
        waitForText("Compatibility review")
        compose.onNodeWithText("Compatibility review").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("삭제된 감상입니다.").assertExists()
        compose.onNodeWithText("Retained reply").assertExists()
        compose.onNodeWithContentDescription("감상 목록 개수 2").assertExists()
        compose.onNodeWithText("Existing reply").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("답글 2개 모두 보기").performScrollTo().performClick()
        waitForText("Second page reply")
        compose.onNodeWithText("Second page reply").performScrollTo().assertIsDisplayed()
        capture("reply-pages")
        compose.onNodeWithContentDescription("감상에 답글 작성", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithContentDescription("답글").performTextInput("Created reply")
        compose.onNodeWithText("등록").performClick()
        waitForText("Created reply")
        compose.onNodeWithText("Created reply").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("답글 3").assertExists()
        compose.onNodeWithContentDescription("내 답글 수정 또는 삭제").performScrollTo().performClick()
        compose.onNodeWithText("수정").performClick()
        compose.onNodeWithContentDescription("답글").performTextReplacement("Updated reply")
        compose.onNodeWithText("수정 저장").performClick()
        waitForText("Updated reply")
        compose.onNodeWithText("Updated reply").performScrollTo().assertIsDisplayed()
        capture("reply-updated")
        compose.onNodeWithContentDescription("내 답글 수정 또는 삭제").performScrollTo().performClick()
        compose.onNodeWithText("삭제").performClick()
        compose.onNodeWithText("답글을 삭제할까요?").assertExists()
        compose.onNodeWithText("삭제").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Updated reply").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("답글 2").assertExists()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        compose.onNodeWithContentDescription("뒤로 가기").performClick()
        waitForText("Reply compatibility")
        compose.onNodeWithText("Reply compatibility").performClick()
        waitForText("Compatibility review")
        compose.onNodeWithText("Existing reply").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Updated reply").assertDoesNotExist()
        compose.onNodeWithText("삭제된 감상입니다.").assertExists()
        compose.onNodeWithText("Retained reply").assertExists()
        compose.onNodeWithContentDescription("감상 목록 개수 2").assertExists()
        capture("reply-reentry")
        compose.onNodeWithContentDescription("내 감상 수정 또는 삭제").performScrollTo().performClick()
        compose.onNodeWithText("삭제").performClick()
        compose.onNodeWithText("감상을 삭제할까요?").assertExists()
        compose.onNodeWithText("삭제").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Compatibility review").fetchSemanticsNodes().isEmpty() }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("삭제된 감상입니다.").fetchSemanticsNodes().size == 2 }
        compose.onNodeWithText("Existing reply").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("감상 목록 개수 2").assertExists()
        compose.onNodeWithContentDescription("감상에 답글 작성", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("내 감상 수정 또는 삭제").assertDoesNotExist()
        compose.onAllNodesWithContentDescription("감상 좋아요").assertAll(isNotEnabled())
        capture("deleted-review-retained")
        val state = JSONObject(URI("http://10.0.2.2:19063/__reply-compat/state").toURL().readText())
        val requests = state.getJSONArray("requests")
        val calls = (0 until requests.length()).map { requests.getJSONObject(it) }
        assertEquals(1, calls.count { it.getString("method") == "PATCH" })
        assertEquals(2, calls.count { it.getString("method") == "DELETE" })
        assertTrue(calls.any { it.getString("path").contains("page=2") })
        assertEquals(2, state.getJSONArray("replies").length())
        assertTrue(state.getBoolean("review_deleted"))
    }

    private fun waitForText(text: String) = compose.waitUntil(15_000) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "563-$name.png")
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
