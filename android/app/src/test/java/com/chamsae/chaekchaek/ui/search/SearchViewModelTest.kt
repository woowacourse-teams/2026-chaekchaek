package com.chamsae.chaekchaek.ui.search

import com.chamsae.chaekchaek.data.ArchivedBook
import com.chamsae.chaekchaek.data.LibraryRepository
import com.chamsae.chaekchaek.data.ReadingStatus
import com.chaekchaek.app.domain.book.BookSearchRepository
import com.chaekchaek.app.domain.book.BookSearchResult
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
          isSignedIn = { true },
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
  fun `register delegates the search result to the library repository`() =
    runTest {
      Dispatchers.setMain(StandardTestDispatcher(testScheduler))
      try {
        val libraryRepository = FakeLibraryRepository()
        val viewModel = SearchViewModel(BookSearchRepository { emptyList() }, libraryRepository) { true }
        val searchResult = book("검색 제목", "2026").copy(isbn13 = "9780000000001", category = "소설", totalPages = 320)

        viewModel.register(searchResult)
        advanceUntilIdle()

        val saved = libraryRepository.items.value.single()
        assertEquals("9780000000001", saved.id)
        assertEquals("검색 제목", saved.title)
        assertEquals("소설", saved.category)
        assertEquals(320, saved.totalPages)
      } finally {
        Dispatchers.resetMain()
      }
    }

  @Test
  fun `로그아웃 등록은 책을 보류하고 로그인 후 한 번만 재개한다`() =
    runTest {
      Dispatchers.setMain(StandardTestDispatcher(testScheduler))
      try {
        var signedIn = false
        val libraryRepository = FakeLibraryRepository()
        val viewModel = SearchViewModel(BookSearchRepository { emptyList() }, libraryRepository) { signedIn }
        val searchResult = book("보류할 책", "2026").copy(isbn13 = "9780000000002")

        viewModel.register(searchResult)
        advanceUntilIdle()

        assertEquals(searchResult, viewModel.pendingRegistration.value)
        assertEquals(emptyList<ArchivedBook>(), libraryRepository.items.value)

        signedIn = true
        viewModel.resumeRegistration()
        viewModel.resumeRegistration()
        advanceUntilIdle()

        assertEquals(null, viewModel.pendingRegistration.value)
        assertEquals(listOf("9780000000002"), libraryRepository.items.value.map(ArchivedBook::id))
      } finally {
        Dispatchers.resetMain()
      }
    }

  @Test
  fun `로그인 후 등록 실패는 보류를 유지해 재시도한다`() =
    runTest {
      Dispatchers.setMain(StandardTestDispatcher(testScheduler))
      try {
        var signedIn = false
        val libraryRepository = FakeLibraryRepository()
        val viewModel = SearchViewModel(BookSearchRepository { emptyList() }, libraryRepository) { signedIn }
        val searchResult = book("재시도할 책", "2026").copy(isbn13 = "9780000000004")

        viewModel.register(searchResult)
        signedIn = true
        libraryRepository.addFailure = IllegalStateException("등록 실패")

        assertEquals("등록 실패", runCatching { viewModel.resumeRegistration() }.exceptionOrNull()?.message)
        assertEquals(searchResult, viewModel.pendingRegistration.value)

        libraryRepository.addFailure = null
        viewModel.resumeRegistration()

        assertEquals(null, viewModel.pendingRegistration.value)
        assertEquals(listOf("9780000000004"), libraryRepository.items.value.map(ArchivedBook::id))
      } finally {
        Dispatchers.resetMain()
      }
    }

  @Test
  fun `로그인 취소는 보류만 지우고 검색 결과를 유지한다`() =
    runTest {
      Dispatchers.setMain(StandardTestDispatcher(testScheduler))
      try {
        val searchResult = book("검색 결과", "2026").copy(isbn13 = "9780000000003")
        val libraryRepository = FakeLibraryRepository()
        val viewModel = SearchViewModel(BookSearchRepository { listOf(searchResult) }, libraryRepository) { false }

        viewModel.search("검색")
        advanceUntilIdle()
        val searchState = viewModel.uiState.value
        viewModel.register(searchResult)
        viewModel.cancelRegistration()
        advanceUntilIdle()

        assertEquals(null, viewModel.pendingRegistration.value)
        assertEquals(emptyList<ArchivedBook>(), libraryRepository.items.value)
        assertEquals(searchState, viewModel.uiState.value)
      } finally {
        Dispatchers.resetMain()
      }
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
  var addFailure: Exception? = null
  override val items: StateFlow<List<ArchivedBook>> = mutableItems
  override val anonymousReviews = MutableStateFlow(true)
  override val nickname = MutableStateFlow("")

  override suspend fun add(book: ArchivedBook): Long? {
    addFailure?.let { throw it }
    mutableItems.value += book
    return book.bookId
  }

  override fun remove(bookIds: Set<String>) = Unit

  override fun changeStatus(bookIds: Set<String>, status: ReadingStatus) = Unit

  override fun setAnonymousReviews(anonymous: Boolean, nickname: String) = Unit
}
