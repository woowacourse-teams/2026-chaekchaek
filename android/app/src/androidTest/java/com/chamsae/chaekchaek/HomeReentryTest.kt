package com.chamsae.chaekchaek

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chaekchaek.app.domain.book.BookId
import com.chaekchaek.app.domain.feed.FeedRepository
import com.chaekchaek.app.domain.feed.FeedSection
import com.chaekchaek.app.domain.feed.HomeFeed
import com.chaekchaek.app.domain.feed.ReadingBook
import com.chaekchaek.app.domain.feed.TrendingBook
import com.chaekchaek.app.presentation.home.HomeUiState
import com.chaekchaek.app.presentation.home.HomeViewModel
import com.chaekchaek.app.ui.home.HomeScreen
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Clock

@RunWith(AndroidJUnit4::class)
class HomeReentryTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun firstGuestEntryReusesInitialFeed() {
        val repository = HomeReentryFeedRepository()
        val viewModel = HomeViewModel(repository, Clock.System)

        compose.setContent {
            ChaekchaekTheme {
                LaunchedEffect(Unit) { viewModel.authenticate(null) }
                HomeScreen(homeViewModel = viewModel, myDisplayName = "")
            }
        }

        compose.onNodeWithText("지금 읽고 있는 책이 있으세요?").assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf<String?>(null), repository.accessTokens.toList()) }
    }

    @Test
    fun reentryWithSameTokenRemovesBookDeletedFromLibrary() {
        val repository = HomeReentryFeedRepository(readingBook = readingBook())
        val viewModel = HomeViewModel(repository, Clock.System)
        val showHome = mutableStateOf(true)

        compose.setContent {
            ChaekchaekTheme {
                LaunchedEffect(Unit) { viewModel.authenticate("access-token") }
                val holder = rememberSaveableStateHolder()
                if (showHome.value) {
                    holder.SaveableStateProvider("Home") {
                        HomeScreen(homeViewModel = viewModel, myDisplayName = "")
                    }
                }
            }
        }
        compose.onNodeWithText("삭제 전 읽던 책").assertIsDisplayed()
        var requestsBeforeReentry = 0
        compose.runOnIdle {
            requestsBeforeReentry = repository.accessTokens.size
            showHome.value = false
        }
        compose.waitForIdle()
        compose.runOnIdle {
            repository.readingBook = null
            showHome.value = true
        }

        compose.waitUntil(timeoutMillis = 3_000) {
            repository.accessTokens.size == requestsBeforeReentry + 1 &&
                (viewModel.uiState.value as? HomeUiState.Content)?.readingBook == null
        }
        compose.onAllNodesWithText("삭제 전 읽던 책").assertCountEquals(0)
        compose.onNodeWithText("지금 읽고 있는 책이 있으세요?").assertIsDisplayed()
        compose.runOnIdle { assertEquals("access-token", repository.accessTokens.last()) }
    }

    @Test
    fun tokenRefreshWhileHomeIsVisibleLoadsOnlyOnce() {
        val repository = HomeReentryFeedRepository()
        val viewModel = HomeViewModel(repository, Clock.System)
        val accessToken = mutableStateOf<String?>(null)

        compose.setContent {
            ChaekchaekTheme {
                LaunchedEffect(accessToken.value) { viewModel.authenticate(accessToken.value) }
                HomeScreen(homeViewModel = viewModel, myDisplayName = "")
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { accessToken.value = "refreshed-access-token" }
        compose.waitForIdle()

        compose.runOnIdle {
            assertEquals(listOf(null, "refreshed-access-token"), repository.accessTokens.toList())
        }
    }

    @Test
    fun firstHomeEntryAfterStartingOnRestoredLibraryLoadsLatestFeed() {
        val repository = HomeReentryFeedRepository(readingBook = readingBook())
        val restoration = StateRestorationTester(compose)
        lateinit var showHome: MutableState<Boolean>
        lateinit var viewModel: HomeViewModel

        restoration.setContent {
            ChaekchaekTheme {
                val currentViewModel = remember { HomeViewModel(repository, Clock.System) }
                val currentShowHome = rememberSaveable { mutableStateOf(true) }
                SideEffect {
                    viewModel = currentViewModel
                    showHome = currentShowHome
                }
                LaunchedEffect(Unit) { currentViewModel.authenticate("access-token") }
                LaunchedEffect(currentShowHome.value) {
                    if (!currentShowHome.value) currentViewModel.onHomeHidden()
                }
                val holder = rememberSaveableStateHolder()
                if (currentShowHome.value) {
                    holder.SaveableStateProvider("Home") {
                        HomeScreen(homeViewModel = currentViewModel, myDisplayName = "")
                    }
                }
            }
        }
        compose.onNodeWithText("삭제 전 읽던 책").assertIsDisplayed()
        val previousViewModel = viewModel
        compose.runOnIdle { showHome.value = false }
        compose.waitForIdle()
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        var requestsBeforeEntry = 0
        compose.runOnIdle {
            assertNotSame(previousViewModel, viewModel)
            assertEquals(false, showHome.value)
            requestsBeforeEntry = repository.accessTokens.size
            repository.readingBook = null
            showHome.value = true
        }

        compose.waitUntil(timeoutMillis = 3_000) {
            repository.accessTokens.size == requestsBeforeEntry + 1 &&
                (viewModel.uiState.value as? HomeUiState.Content)?.readingBook == null
        }
        compose.onAllNodesWithText("삭제 전 읽던 책").assertCountEquals(0)
        compose.onNodeWithText("지금 읽고 있는 책이 있으세요?").assertIsDisplayed()
        compose.runOnIdle { assertEquals("access-token", repository.accessTokens.last()) }
    }
}

private class HomeReentryFeedRepository(
    var readingBook: ReadingBook? = null,
) : FeedRepository {
    val accessTokens = CopyOnWriteArrayList<String?>()

    override suspend fun homeFeed(accessToken: String?): HomeFeed {
        accessTokens += accessToken
        return HomeFeed(
            sections = listOf(
                FeedSection.TrendingBooks(
                    books = listOf(TrendingBook(BookId("7"), "인기 책", "", 1, 0)),
                    totalCount = 1,
                ),
            ),
            readingBook = readingBook,
        )
    }
}

private fun readingBook() = ReadingBook(
    bookId = BookId("42"),
    isbn13 = "9780000000042",
    title = "삭제 전 읽던 책",
    coverId = "",
    currentPage = 132,
    totalPages = 320,
)
