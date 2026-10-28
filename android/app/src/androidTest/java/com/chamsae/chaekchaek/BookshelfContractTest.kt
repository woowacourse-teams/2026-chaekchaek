package com.chamsae.chaekchaek

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import java.io.File
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
    private val origin = ApiConfiguration.current.baseUrl

    @Test fun searchAndCreateShelfWhileChoosingDestination() {
        Assume.assumeTrue("bookshelf-contract local fixture가 필요합니다", ApiConfiguration.environment == "local" && origin in setOf("http://10.0.2.2:19068", "http://127.0.0.1:19068"))
        request("/__bookshelf/reset", "POST")
        compose.setContent { App(AuthPlatformCallbacks({ it(null, null) }, { "fixture-refresh" }, {}, {})) }
        waitText("발견")
        compose.onNodeWithText("발견").performClick()
        waitTag("search-query-field")
        compose.onNodeWithTag("search-query-field").performTextInput("검증")
        compose.onNodeWithTag("search-query-field").performImeAction()
        waitText("검증 도서 36")
        waitText("책 담기")
        compose.onNodeWithText("책 담기").performClick()
        waitText("담을 책장 고르기")
        compose.onNodeWithTag("shelf-choice-create").performScrollTo().performClick()
        waitTag("bookshelfName")
        compose.onNodeWithTag("bookshelfName").performTextInput("담기 중 생성")
        compose.onNodeWithText("만들기").performScrollTo().performClick()
        waitNameSheetClosed()
        waitText("담을 책장 고르기")
        compose.onNodeWithText("담기 중 생성").assertExists()
        assertFalse(requested("POST", "/api/v1/libraries/4"))
        compose.onNodeWithText("이 책장에 담기").performScrollTo().performClick()
        compose.waitUntil(15_000) { requested("POST", "/api/v1/libraries/4") }
        val shelves = JSONObject(request("/__bookshelf/state")).getJSONArray("shelves")
        val created = (0 until shelves.length()).map { shelves.getJSONObject(it) }.single { it.getString("name") == "담기 중 생성" }
        assertEquals(36, created.getJSONArray("books").getInt(0))
    }

    @Test fun createRenameAddRemove() {
        Assume.assumeTrue("bookshelf-contract local fixture가 필요합니다", ApiConfiguration.environment == "local" && origin in setOf("http://10.0.2.2:19068", "http://127.0.0.1:19068"))
        request("/__bookshelf/reset", "POST")
        compose.setContent { App(AuthPlatformCallbacks({ it(null, null) }, { "fixture-refresh" }, {}, {})) }
        waitText("내 서재")
        compose.onNodeWithText("내 서재").performClick()
        waitTag("bookshelf-1")
        compose.onNodeWithTag("bookshelf-menu-1").performClick()
        compose.onNodeWithText("책장 이름 바꾸기").performClick()
        compose.onNodeWithTag("bookshelfName").performTextReplacement("나의 책장")
        compose.onNodeWithText("저장").performScrollTo().performClick()
        waitNameSheetClosed()
        waitText("나의 책장")
        compose.onNodeWithTag("bookshelf-create").performClick()
        compose.onNodeWithTag("bookshelfName").performTextInput("올해읽을책")
        compose.onNodeWithText("만들기").performScrollTo().performClick()
        waitNameSheetClosed()
        waitTag("bookshelf-1")
        compose.onNodeWithTag("bookshelf-1").performClick()
        waitText("+ 책 담기")
        compose.onNodeWithText("+ 책 담기").performScrollTo().performClick()
        compose.onNodeWithText("검증 도서 3").performScrollTo().performClick()
        waitText("담을 책장 고르기")
        compose.onNodeWithText("이 책장에 담기").performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("담을 책장 고르기").fetchSemanticsNodes().isEmpty() }
        waitText("3권")
        compose.waitUntil(15_000) { compose.onAllNodesWithText("내 서재에 담았어요").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("편집").performScrollTo().performClick()
        compose.onAllNodesWithText("빼기").onFirst().performScrollTo().performClick()
        compose.waitUntil(15_000) { requested("DELETE", "/api/v1/libraries/1/books/1") }
        waitText("2권")
        compose.onNodeWithText("완료").performScrollTo().performClick()
        val state = JSONObject(request("/__bookshelf/state"))
        val shelves = state.getJSONArray("shelves")
        assertTrue((0 until shelves.length()).any { shelves.getJSONObject(it).getString("name") == "올해읽을책" })
        val requests = state.getJSONArray("requests")
        val calls = (0 until requests.length()).map { requests.getJSONObject(it) }
        assertTrue(calls.any { it.getString("method") == "POST" && it.getString("path") == "/api/v1/libraries/1" })
        assertTrue(calls.any { it.getString("method") == "DELETE" && it.getString("path").contains("/books/") })
    }

    @Test fun shelfHandleDragCancelRestoresAndDropSavesOnce() {
        Assume.assumeTrue("bookshelf-contract local fixture가 필요합니다", ApiConfiguration.environment == "local" && origin in setOf("http://10.0.2.2:19068", "http://127.0.0.1:19068"))
        request("/__bookshelf/reset", "POST")
        compose.setContent { App(AuthPlatformCallbacks({ it(null, null) }, { "fixture-refresh" }, {}, {})) }
        waitText("내 서재")
        compose.onNodeWithText("내 서재").performClick()
        waitTag("bookshelf-reorder-2")
        val first = compose.onNodeWithTag("bookshelf-reorder-1").fetchSemanticsNode().boundsInRoot.center
        val second = compose.onNodeWithTag("bookshelf-reorder-2").fetchSemanticsNode().boundsInRoot.center
        val delta = first - second
        compose.onNodeWithTag("bookshelf-reorder-2").performTouchInput {
            down(center)
            moveBy(delta, delayMillis = 500)
            cancel()
        }
        compose.waitForIdle()
        assertFalse("드래그 취소는 서버 순서를 바꾸지 않음", requested("PATCH", "/api/v1/libraries/order"))
        compose.onNodeWithTag("bookshelf-reorder-2").performTouchInput {
            down(center)
            moveBy(delta, delayMillis = 500)
            up()
        }
        compose.waitUntil(15_000) { requested("PATCH", "/api/v1/libraries/order") }
        val state = JSONObject(request("/__bookshelf/state"))
        val shelves = state.getJSONArray("shelves")
        assertEquals(listOf(2, 1, 3), (0 until shelves.length()).map { shelves.getJSONObject(it).getInt("libraryId") })
        val calls = state.getJSONArray("requests")
        assertEquals("드롭 저장은 1회", 1, (0 until calls.length()).count {
            calls.getJSONObject(it).getString("method") == "PATCH" && calls.getJSONObject(it).getString("path") == "/api/v1/libraries/order"
        })
    }

    @Test fun nativeImageSaveAndShare() {
        Assume.assumeTrue("bookshelf-contract local fixture가 필요합니다", ApiConfiguration.environment == "local" && origin in setOf("http://10.0.2.2:19068", "http://127.0.0.1:19068"))
        request("/__bookshelf/reset", "POST")
        compose.setContent { App(AuthPlatformCallbacks({ it(null, null) }, { "fixture-refresh" }, {}, {})) }
        waitText("내 서재")
        compose.onNodeWithText("내 서재").performClick()
        waitTag("bookshelf-1")
        compose.onNodeWithTag("bookshelf-1").performClick()
        compose.waitUntil(15_000) { compose.onAllNodes(hasContentDescription("이미지 공유") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("이미지 공유").performScrollTo().performClick()
        waitText("책장을 이미지로")
        compose.onNodeWithText("이미지 저장").performScrollTo().assertIsDisplayed()
        compose.waitUntil(15_000) { compose.onAllNodes(hasText("이미지 저장") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("이미지 저장").performClick()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        compose.waitUntil(60_000) { automation.rootInActiveWindow?.packageName?.toString()?.endsWith("documentsui") == true }
        compose.waitUntil(60_000) { findSave(automation.rootInActiveWindow) != null }
        val name = findFileName(automation.rootInActiveWindow)
        assertNotNull("네이티브 저장 파일 이름", name)
        assertTrue(name!!.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "책장-계측-${System.currentTimeMillis()}.png")
        }))
        val save = findSave(automation.rootInActiveWindow)
        assertNotNull("네이티브 파일 저장 버튼", save)
        assertTrue(save!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        compose.waitUntil(60_000) { automation.rootInActiveWindow?.packageName?.toString() == instrumentation.targetContext.packageName }
        waitText("이미지 저장을 완료했어요")
        val screenshot = File(instrumentation.targetContext.getExternalFilesDir(null), "bookshelf-image-preview.png")
        automation.takeScreenshot()?.let { bitmap -> screenshot.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }

        assertFalse("공유 준비에서 감상 API를 호출하지 않음", requested("GET", "/api/v1/books/1/reviews"))
        compose.onNodeWithTag("bookshelf-image-share", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitUntil(60_000) {
            automation.rootInActiveWindow?.packageName?.toString() in setOf("android", "com.android.intentresolver")
        }
        val exported = instrumentation.targetContext.cacheDir.resolve("bookshelf-images").listFiles()?.maxByOrNull { it.lastModified() }
        assertNotNull("네이티브 공유에 전달할 PNG", exported)
        val dimensions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(exported!!.absolutePath, dimensions)
        assertTrue("전체 책장 이미지 너비", dimensions.outWidth > 0)
        assertTrue("감상 칼럼을 제외한 두 권 책장 이미지 높이", dimensions.outHeight < dimensions.outWidth * 2)
        automation.takeScreenshot()?.let { bitmap ->
            File(instrumentation.targetContext.getExternalFilesDir(null), "bookshelf-native-share.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    @Test fun nativeShareWithoutSave() {
        Assume.assumeTrue("bookshelf-contract local fixture가 필요합니다", ApiConfiguration.environment == "local" && origin in setOf("http://10.0.2.2:19068", "http://127.0.0.1:19068"))
        request("/__bookshelf/reset", "POST")
        compose.setContent { App(AuthPlatformCallbacks({ it(null, null) }, { "fixture-refresh" }, {}, {})) }
        waitText("내 서재")
        compose.onNodeWithText("내 서재").performClick()
        waitTag("bookshelf-1")
        compose.onNodeWithTag("bookshelf-1").performClick()
        compose.waitUntil(30_000) { compose.onAllNodes(hasContentDescription("이미지 공유") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("이미지 공유").performScrollTo().performClick()
        waitText("책장을 이미지로")
        compose.onNodeWithTag("bookshelf-image-share", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.waitForIdle()
        try {
            compose.waitUntil(30_000) { compose.onAllNodes(hasTestTag("bookshelf-image-share") and isEnabled(), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        } catch (error: Throwable) {
            val roots = compose.onAllNodes(isRoot(), useUnmergedTree = true)
            repeat(roots.fetchSemanticsNodes().size) { roots[it].printToLog("BookshelfShareReadiness") }
            throw error
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = instrumentation.targetContext.cacheDir.resolve("bookshelf-images")
        val previousFiles = directory.listFiles()?.map { it.name }?.toSet().orEmpty()
        compose.onNodeWithTag("bookshelf-image-share", useUnmergedTree = true).performScrollTo().performClick()
        val automation = instrumentation.uiAutomation
        compose.waitUntil(60_000) {
            automation.rootInActiveWindow?.packageName?.toString() in setOf("android", "com.android.intentresolver")
        }
        automation.waitForIdle(500, 10_000)
        assertTrue("시스템 공유 창 유지", automation.rootInActiveWindow?.packageName?.toString() in setOf("android", "com.android.intentresolver"))
        val exported = directory.listFiles()?.singleOrNull { it.name !in previousFiles }
        assertNotNull("이번 공유에서 생성한 PNG", exported)
        val dimensions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(exported!!.absolutePath, dimensions)
        assertTrue("책장 이미지 너비", dimensions.outWidth > 0)
        assertTrue("감상 칼럼을 제외한 두 권 책장 이미지 높이", dimensions.outHeight < dimensions.outWidth * 2)
        assertFalse("직접 공유에서 감상 API를 호출하지 않음", requested("GET", "/api/v1/books/1/reviews"))
        automation.takeScreenshot()?.let { bitmap ->
            File(instrumentation.targetContext.getExternalFilesDir(null), "bookshelf-native-share.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    private fun findFileName(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable && node.className?.toString() == "android.widget.EditText") return node
        for (index in 0 until node.childCount) findFileName(node.getChild(index))?.let { return it }
        return null
    }

    private fun findSave(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEnabled && node.isClickable && (node.viewIdResourceName?.endsWith(":id/action_menu_save") == true || node.text?.toString()?.equals("Save", ignoreCase = true) == true || node.text?.toString() == "저장")) return node
        for (index in 0 until node.childCount) findSave(node.getChild(index))?.let { return it }
        return null
    }

    private fun waitNameSheetClosed() = compose.waitUntil(15_000) { compose.onAllNodesWithTag("bookshelfName").fetchSemanticsNodes().isEmpty() }
    private fun waitText(text: String) = compose.waitUntil(15_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    private fun waitTag(tag: String) = compose.waitUntil(15_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun requested(method: String, pathPrefix: String): Boolean {
        val calls = JSONObject(request("/__bookshelf/state")).getJSONArray("requests")
        return (0 until calls.length()).any {
            val call = calls.getJSONObject(it)
            call.getString("method") == method && call.getString("path").startsWith(pathPrefix)
        }
    }

    private fun request(path: String, method: String = "GET"): String {
        val connection = URI(origin + path).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000; connection.readTimeout = 10_000; connection.requestMethod = method
        return try { connection.inputStream.bufferedReader().use { it.readText() } } finally { connection.disconnect() }
    }
}
