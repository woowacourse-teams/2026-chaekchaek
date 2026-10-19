package com.chamsae.chaekchaek

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chaekchaek.app.presentation.common.RefreshUiState
import com.chaekchaek.app.ui.common.ChaekRefreshLayout
import com.chaekchaek.app.ui.common.ListBoundary
import com.chaekchaek.app.ui.common.RefreshableEmptyContent
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PullRefreshTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun emptyViewportRefreshesOnceAndKeepsTheHeaderFixed() {
        var requests = 0
        val state = mutableStateOf(RefreshUiState())
        compose.setContent {
            ChaekchaekTheme {
                Column {
                    Box(Modifier.height(70.dp).testTag("fixed-header")) { Text("고정 헤더") }
                    ChaekRefreshLayout(state.value, {
                        requests++
                        state.value = RefreshUiState(isRefreshing = true)
                    }, Modifier.weight(1f).testTag("refresh-body")) {
                        RefreshableEmptyContent { Text("빈 화면") }
                    }
                }
            }
        }
        val headerBefore = compose.onNodeWithTag("fixed-header").fetchSemanticsNode().boundsInRoot
        val emptyBefore = compose.onNodeWithText("빈 화면").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("refresh-body").performTouchInput { swipeDown() }
        compose.onNodeWithTag("pull-refresh-indicator").assertIsDisplayed()
        compose.onNodeWithText("갱신 요청 중").assertIsDisplayed()
        compose.onNodeWithTag("pull-refresh-spinner", useUnmergedTree = true).assertDoesNotExist()
        val body = compose.onNodeWithTag("refresh-body").fetchSemanticsNode().boundsInRoot
        val indicator = compose.onNodeWithTag("pull-refresh-indicator").fetchSemanticsNode().boundsInRoot
        assertEquals(body.center.x, indicator.center.x, 0.5f)
        assertEquals(headerBefore, compose.onNodeWithTag("fixed-header").fetchSemanticsNode().boundsInRoot)
        assertEquals(emptyBefore, compose.onNodeWithText("빈 화면").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithText("빈 화면").assertIsDisplayed()
        compose.onNodeWithTag("fixed-header").assertIsDisplayed()
        compose.onNodeWithTag("refresh-body").performTouchInput { swipeDown() }
        compose.runOnIdle { assertEquals(1, requests) }
        compose.runOnIdle { state.value = RefreshUiState() }
        compose.onNodeWithTag("pull-refresh-indicator").assertDoesNotExist()
    }

    @Test
    fun shortPullDoesNotRefreshAndDisabledEditingDoesNotRefresh() {
        var requests = 0
        val enabled = mutableStateOf(true)
        compose.setContent {
            ChaekchaekTheme {
                ChaekRefreshLayout(RefreshUiState(), { requests++ }, Modifier.testTag("refresh-body"), enabled.value) {
                    RefreshableEmptyContent { Text("짧은 목록") }
                }
            }
        }
        compose.onNodeWithTag("refresh-body").performTouchInput {
            swipe(Offset(centerX, centerY), Offset(centerX, centerY + 35f), durationMillis = 500)
        }
        compose.runOnIdle { assertEquals(0, requests); enabled.value = false }
        compose.onNodeWithTag("refresh-body").performTouchInput { swipeDown() }
        compose.runOnIdle { assertEquals(0, requests) }
    }

    @Test
    fun editingToggleRetainsContentStateAndRefreshWorksAfterReenabling() {
        val enabled = mutableStateOf(true)
        val state = mutableStateOf(RefreshUiState())
        var requests = 0
        compose.setContent {
            ChaekchaekTheme {
                ChaekRefreshLayout(state.value, {
                    requests++
                    state.value = RefreshUiState(isRefreshing = true)
                }, Modifier.testTag("refresh-body"), enabled.value) {
                    val selection = remember { mutableStateOf(false) }
                    RefreshableEmptyContent {
                        Text(if (selection.value) "선택 유지" else "선택 전",
                            Modifier.clickable(indication = null, interactionSource = null) { selection.value = !selection.value })
                    }
                }
            }
        }
        compose.onNodeWithText("선택 전").performClick()
        compose.runOnIdle { enabled.value = false }
        compose.onNodeWithText("선택 유지").assertIsDisplayed()
        compose.onNodeWithTag("refresh-body").performTouchInput { swipeDown() }
        compose.runOnIdle { assertEquals(0, requests); enabled.value = true }
        compose.onNodeWithText("선택 유지").assertIsDisplayed()
        compose.onNodeWithTag("refresh-body").performTouchInput { swipeDown() }
        compose.runOnIdle { assertEquals(1, requests) }
        compose.onNodeWithTag("pull-refresh-indicator").assertIsDisplayed()
        compose.runOnIdle { state.value = RefreshUiState(isRefreshing = true, showLoading = true) }
        compose.onNodeWithTag("pull-refresh-indicator").assertIsDisplayed()
        compose.onNodeWithText("새로고침 중이에요").assertIsDisplayed()
        compose.onNodeWithTag("pull-refresh-spinner", useUnmergedTree = true).assertIsDisplayed()
        val shot = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        java.io.File(context.getExternalFilesDir(null), "529-simple-refresh.png").outputStream().use {
            shot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        shot.recycle()
        compose.runOnIdle { state.value = RefreshUiState() }
        compose.onNodeWithTag("pull-refresh-indicator").assertDoesNotExist()
        compose.onNodeWithText("선택 유지").assertIsDisplayed()
    }

    @Test
    fun immediateResponseClosesIndicatorAndAllowsAnotherRefresh() {
        var requests = 0
        compose.setContent {
            ChaekchaekTheme {
                ChaekRefreshLayout(RefreshUiState(), { requests++ }, Modifier.testTag("refresh-body")) {
                    RefreshableEmptyContent { Text("빠른 응답") }
                }
            }
        }
        repeat(2) { index ->
            compose.onNodeWithTag("refresh-body").performTouchInput { swipeDown() }
            compose.runOnIdle { assertEquals(index + 1, requests) }
            compose.onNodeWithTag("pull-refresh-indicator").assertDoesNotExist()
        }
    }

    @Test
    fun bottomPullHidesEndAndDoesNotRefresh() {
        var requests = 0
        compose.setContent {
            ChaekchaekTheme {
                ChaekRefreshLayout(RefreshUiState(), { requests++ }, Modifier.testTag("refresh-body")) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        item { Text("짧은 목록") }
                        item { ListBoundary() }
                    }
                }
            }
        }
        compose.onNodeWithTag("refresh-body").performTouchInput {
            swipe(Offset(centerX, centerY), Offset(centerX, centerY - 180f), durationMillis = 500)
        }
        compose.onNodeWithText("여기까지 모두 봤어요").assertDoesNotExist()
        compose.onNodeWithTag("list-boundary").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, requests) }
    }
}
