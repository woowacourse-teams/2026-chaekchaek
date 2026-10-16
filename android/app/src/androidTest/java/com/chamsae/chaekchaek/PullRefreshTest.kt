package com.chamsae.chaekchaek

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
        compose.onNodeWithTag("refresh-body").performTouchInput { swipeDown() }
        compose.onNodeWithText("갱신 요청 중").assertIsDisplayed()
        compose.onNodeWithText("빈 화면").assertIsDisplayed()
        compose.onNodeWithTag("fixed-header").assertIsDisplayed()
        compose.onNodeWithTag("refresh-body").performTouchInput { swipeDown() }
        compose.runOnIdle { assertEquals(1, requests) }
        compose.runOnIdle { state.value = RefreshUiState() }
        compose.onNodeWithText("갱신 요청 중").assertDoesNotExist()
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
