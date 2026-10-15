package com.chamsae.chaekchaek

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chaekchaek.app.domain.feed.FeedRepository
import com.chaekchaek.app.domain.feed.HomeFeed
import com.chaekchaek.app.presentation.home.HomeUiState
import com.chaekchaek.app.presentation.home.HomeViewModel
import com.chaekchaek.app.ui.home.HomeScreen
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
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

    private fun assertDeveloperToolsEntry(
        viewModel: HomeViewModel,
        expectedState: (HomeUiState) -> Boolean,
    ) {
        var clicks = 0
        compose.setContent {
            ChaekchaekTheme {
                HomeScreen(
                    homeViewModel = viewModel,
                    myDisplayName = "",
                    homeHeaderAction = { DeveloperToolsEntryButton { clicks += 1 } },
                )
            }
        }

        compose.waitUntil(timeoutMillis = 3_000) { expectedState(viewModel.uiState.value) }
        compose.waitForIdle()
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
