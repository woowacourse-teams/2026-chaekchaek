package com.chamsae.chaekchaek.data

import com.chaekchaek.app.domain.book.BookSearchResult
import org.junit.Assert.assertEquals
import org.junit.Test

class BookTest {

  @Test
  fun `search result converts to one unique archived book`() {
    val archived =
      BookSearchResult(
        title = "마션",
        creator = "앤디 위어",
        publisher = "알에이치코리아",
        year = "2026",
        coverUrl = "https://example.com/martian.jpg",
        description = "",
        isbn13 = "9788925588650",
      ).toArchivedBook()

    val items = emptyList<ArchivedBook>().plusIfAbsent(archived).plusIfAbsent(archived)

    assertEquals("9788925588650", archived.id)
    assertEquals(listOf(archived), items)
  }

  @Test
  fun `status change keeps reading progress invariants`() {
    val book =
      ArchivedBook(
        id = "1",
        title = "마션",
        creator = "앤디 위어",
        publisher = "출판사",
        year = "2024",
        coverUrl = "",
        note = "",
        currentPage = 80,
        totalPages = 308,
      )

    assertEquals(0, book.changedTo(ReadingStatus.WantToRead, 1L).currentPage)
    assertEquals(80, book.changedTo(ReadingStatus.Reading, 2L).currentPage)
    assertEquals(308, book.changedTo(ReadingStatus.Finished, 3L).currentPage)
  }
}
