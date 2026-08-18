package com.chamsae.chaekchaek.ui.search

import com.chamsae.chaekchaek.data.ArchivedBook
import com.chamsae.chaekchaek.data.BookSearchRepository
import com.chamsae.chaekchaek.data.BookSearchResult
import com.chamsae.chaekchaek.data.LibraryRepository
import com.chamsae.chaekchaek.data.ReadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        val books = listOf(book("오래된 책", "2021"), book("연도 없음", ""), book("새 책", "2026"))
        val viewModel = SearchViewModel(
          bookSearchRepository = BookSearchRepository { query -> if (query == "없음") emptyList() else books },
          libraryRepository = FakeLibraryRepository(),
        )

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

  @Test
  fun `register delegates the search result to the library repository`() {
    val libraryRepository = FakeLibraryRepository()
    val viewModel = SearchViewModel(BookSearchRepository { emptyList() }, libraryRepository)
    val searchResult = book("검색 제목", "2026").copy(isbn13 = "9780000000001", category = "소설", totalPages = 320)

    viewModel.register(searchResult)

    val saved = libraryRepository.items.value.single()
    assertEquals("9780000000001", saved.id)
    assertEquals("검색 제목", saved.title)
    assertEquals("소설", saved.category)
    assertEquals(320, saved.totalPages)
  }

  private fun book(title: String, year: String) =
    BookSearchResult(
      title = title,
      creator = "저자",
      publisher = "출판사",
      year = year,
      coverUrl = "",
      description = "",
    )
}

private class FakeLibraryRepository : LibraryRepository {
  private val mutableItems = MutableStateFlow(emptyList<ArchivedBook>())
  override val items: StateFlow<List<ArchivedBook>> = mutableItems
  override val anonymousReviews = MutableStateFlow(true)
  override val nickname = MutableStateFlow("")

  override fun add(book: ArchivedBook) {
    mutableItems.value += book
  }

  override fun remove(bookIds: Set<String>) = Unit

  override fun changeStatus(bookIds: Set<String>, status: ReadingStatus) = Unit

  override fun changeProgress(bookId: String, currentPage: Int) = Unit

  override fun setAnonymousReviews(anonymous: Boolean, nickname: String) = Unit
}
