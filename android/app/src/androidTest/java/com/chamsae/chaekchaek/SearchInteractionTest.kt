package com.chamsae.chaekchaek

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.text.TextRange
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chaekchaek.app.domain.book.BookSearchResult
import com.chaekchaek.app.domain.book.BookSearchSort
import com.chaekchaek.app.presentation.home.HomeUiState
import com.chaekchaek.app.ui.search.SearchScreen
import com.chaekchaek.app.ui.search.SearchUiState
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun recentSearchClearsFocusAndPlacesCursorAtTheEnd() {
        val recentQuery = "훨씬 긴 최근 검색어"
        compose.setContent {
            var query by remember { mutableStateOf("책") }
            ChaekchaekTheme {
                SearchScreen(
                    state = SearchUiState.Idle,
                    sort = BookSearchSort.LATEST,
                    query = query,
                    recentSearches = listOf(recentQuery),
                    homeState = HomeUiState.Empty,
                    registeredBookIds = emptySet(),
                    searchResultsListState = rememberLazyListState(),
                    onSearch = { query = it },
                    onQueryChange = { query = it },
                    onClear = { query = "" },
                    onRegister = {},
                    onLoadMore = {},
                    onSortSelect = {},
                )
            }
        }
        val field = compose.onNodeWithTag("search-query-field")
        field.performClick().assertIsFocused()
        field.performTextInputSelection(TextRange(1))

        compose.onNodeWithText(recentQuery).performClick()

        field.assertTextEquals(recentQuery)
            .assertIsNotFocused()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.TextSelectionRange,
                    TextRange(recentQuery.length),
                ),
            )
    }

    @Test
    fun keyboardSearchClearsFocus() {
        compose.setContent {
            var query by remember { mutableStateOf("검색어") }
            ChaekchaekTheme {
                SearchScreen(
                    state = SearchUiState.Idle,
                    sort = BookSearchSort.LATEST,
                    query = query,
                    recentSearches = emptyList(),
                    homeState = HomeUiState.Empty,
                    registeredBookIds = emptySet(),
                    searchResultsListState = rememberLazyListState(),
                    onSearch = { query = it },
                    onQueryChange = { query = it },
                    onClear = { query = "" },
                    onRegister = {},
                    onLoadMore = {},
                    onSortSelect = {},
                )
            }
        }
        val field = compose.onNodeWithTag("search-query-field")

        field.performClick().assertIsFocused().performImeAction()

        field.assertIsNotFocused()
    }

    @Test
    fun contentTouchClearsFocusInEverySearchState() {
        var state by mutableStateOf<SearchUiState>(SearchUiState.Idle)
        compose.setContent {
            var query by remember { mutableStateOf("검색어") }
            ChaekchaekTheme {
                SearchScreen(
                    state = state,
                    sort = BookSearchSort.LATEST,
                    query = query,
                    recentSearches = emptyList(),
                    homeState = HomeUiState.Empty,
                    registeredBookIds = emptySet(),
                    searchResultsListState = rememberLazyListState(),
                    onSearch = { query = it },
                    onQueryChange = { query = it },
                    onClear = { query = "" },
                    onRegister = {},
                    onLoadMore = {},
                    onSortSelect = {},
                )
            }
        }
        val states = listOf(
            SearchUiState.Idle,
            SearchUiState.Loading,
            SearchUiState.Empty,
            SearchUiState.Error("실패"),
            SearchUiState.Success(emptyList(), 0, null),
        )
        val field = compose.onNodeWithTag("search-query-field")
        val content = compose.onNodeWithTag("search-content")

        states.forEach { nextState ->
            compose.runOnIdle { state = nextState }
            field.performClick().assertIsFocused()
            content.performTouchInput { click() }
            field.assertIsNotFocused()
        }
    }

    @Test
    fun contentFocusDismissalDoesNotConsumeBookClick() {
        val book = BookSearchResult(
            title = "클릭할 책",
            creator = "작가",
            publisher = "출판사",
            year = "2026",
            coverUrl = "",
            isbn13 = "9790000000000",
        )
        var bookClicks = 0
        compose.setContent {
            var query by remember { mutableStateOf("검색어") }
            ChaekchaekTheme {
                SearchScreen(
                    state = SearchUiState.Success(listOf(book), 1, null),
                    sort = BookSearchSort.LATEST,
                    query = query,
                    recentSearches = emptyList(),
                    registeredBookIds = emptySet(),
                    searchResultsListState = rememberLazyListState(),
                    onSearch = { query = it },
                    onQueryChange = { query = it },
                    onClear = { query = "" },
                    onRegister = {},
                    onLoadMore = {},
                    onSortSelect = {},
                    onBookClick = { bookClicks += 1 },
                )
            }
        }
        val field = compose.onNodeWithTag("search-query-field")
        field.performClick().assertIsFocused()

        compose.onNodeWithText(book.title).performClick()

        field.assertIsNotFocused()
        compose.runOnIdle { assertEquals(1, bookClicks) }
    }
}
