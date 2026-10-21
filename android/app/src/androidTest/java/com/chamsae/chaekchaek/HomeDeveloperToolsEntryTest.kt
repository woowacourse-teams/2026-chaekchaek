package com.chamsae.chaekchaek

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chaekchaek.app.domain.feed.FeedRepository
import com.chaekchaek.app.domain.feed.HomeFeed
import com.chaekchaek.app.presentation.home.HomeUiState
import com.chaekchaek.app.presentation.home.HomeViewModel
import com.chaekchaek.app.ui.home.HomeScreen
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Clock

@RunWith(AndroidJUnit4::class)
class HomeDeveloperToolsEntryTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun loadingStateKeepsDeveloperToolsEntryClickable() {
        val response = CompletableDeferred<HomeFeed>()
        val viewModel = HomeViewModel(
            feedRepository = feedRepository { response.await() },
            clock = Clock.System,
        )

        assertDeveloperToolsEntry(viewModel) { it == HomeUiState.Loading }
        response.complete(HomeFeed(emptyList()))
    }

    @Test
    fun emptyStateKeepsDeveloperToolsEntryClickable() {
        val viewModel = HomeViewModel(
            feedRepository = feedRepository { HomeFeed(emptyList()) },
            clock = Clock.System,
        )

        assertDeveloperToolsEntry(viewModel) { it == HomeUiState.Empty }
    }

    @Test
    fun failureStateKeepsDeveloperToolsEntryClickableAndRetries() {
        var requests = 0
        val viewModel = HomeViewModel(
            feedRepository = feedRepository {
                requests += 1
                if (requests == 1) error("home load failed")
                HomeFeed(emptyList())
            },
            clock = Clock.System,
        )
        var developerToolsClicks = 0
        compose.setContent {
            ChaekchaekTheme {
                com.chaekchaek.app.ui.common.ErrorRecoveryEffect(viewModel.errors)
                HomeScreen(
                    homeViewModel = viewModel,
                    myDisplayName = "",
                    homeHeaderAction = {
                        DeveloperToolsEntryButton { developerToolsClicks += 1 }
                    },
                )
            }
        }

        compose.waitUntil(timeoutMillis = 3_000) {
            viewModel.errors.notice.value != null
        }
        compose.waitForIdle()
        captureFailurePanel()
        compose.onNodeWithText("닫기").performTouchInput { click() }
        compose.onNodeWithText("DEV")
            .assertIsDisplayed()
            .performTouchInput { click() }
        compose.runOnIdle { viewModel.retry() }

        compose.waitUntil(timeoutMillis = 3_000) {
            viewModel.uiState.value == HomeUiState.Empty && requests == 2
        }
        compose.runOnIdle { assertEquals(1, developerToolsClicks) }
    }

    private fun captureFailurePanel() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "home-failure-panel.png").outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        screenshot.recycle()
    }

    private fun assertDeveloperToolsEntry(
        viewModel: HomeViewModel,
        expectedState: (HomeUiState) -> Boolean,
    ) {
        var clicks = 0
        compose.setContent {
            ChaekchaekTheme {
                com.chaekchaek.app.ui.common.ErrorRecoveryEffect(viewModel.errors)
                HomeScreen(
                    homeViewModel = viewModel,
                    myDisplayName = "",
                    homeHeaderAction = { DeveloperToolsEntryButton { clicks += 1 } },
                )
            }
        }

        compose.waitUntil(timeoutMillis = 3_000) { expectedState(viewModel.uiState.value) }
        compose.waitForIdle()
        if (viewModel.errors.notice.value != null) {
            compose.onNodeWithText("닫기").performTouchInput { click() }
        }
        compose.onNodeWithText("DEV")
            .assertIsDisplayed()
            .performTouchInput { click() }

        compose.runOnIdle { assertEquals(1, clicks) }
    }
}

private fun feedRepository(load: suspend () -> HomeFeed): FeedRepository =
    object : FeedRepository {
        override suspend fun homeFeed(accessToken: String?): HomeFeed = load()
    }
