package com.chaekchaek.app.ui.search

import com.chaekchaek.app.data.BookSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
  @Test
  fun `search exposes empty state and sorts results by newest year`() =
    runTest {
      Dispatchers.setMain(StandardTestDispatcher(testScheduler))
      try {
        val books =
          listOf(
            book("오래된 책", "2021"),
            book("연도 없음", ""),
            book("새 책", "2026"),
          )
        val viewModel = SearchViewModel { query -> if (query == "없음") emptyList() else books }

        viewModel.search("없음")
        advanceUntilIdle()
        assertEquals(SearchUiState.Empty, viewModel.uiState.value)

        viewModel.search("책")
        advanceUntilIdle()
        assertEquals(listOf("새 책", "오래된 책", "연도 없음"), (viewModel.uiState.value as SearchUiState.Success).results.map { it.title })
      } finally {
        Dispatchers.resetMain()
      }
    }

  private fun book(
    title: String,
    year: String,
  ) =
    BookSearchResult(
      title = title,
      creator = "저자",
      publisher = "출판사",
      year = year,
      coverUrl = "",
      description = "",
    )
}
