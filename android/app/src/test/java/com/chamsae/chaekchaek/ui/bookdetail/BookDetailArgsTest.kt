package com.chamsae.chaekchaek.ui.bookdetail

import com.chaekchaek.app.domain.book.BookId
import com.chaekchaek.app.presentation.home.ReadingBookUiModel
import com.chaekchaek.app.presentation.home.TrendingBookUiModel
import com.chamsae.chaekchaek.data.ArchivedBook
import com.chamsae.chaekchaek.data.BookSearchResult
import org.junit.Assert.assertEquals
import org.junit.Test

class BookDetailArgsTest {
  @Test
  fun `search result keeps bibliographic data for detail navigation`() {
    val args =
      BookSearchResult(
        title = "마션",
        creator = "앤디 위어",
        publisher = "알에이치코리아",
        year = "2026",
        coverUrl = "https://example.com/martian.jpg",
        description = "",
        isbn13 = "9788925588650",
        category = "SF",
        totalPages = 308,
      ).toBookDetailArgs()

    assertEquals("9788925588650", args.id)
    assertEquals("마션", args.title)
    assertEquals("앤디 위어", args.creator)
    assertEquals("SF", args.category)
    assertEquals(308, args.totalPages)
  }

  @Test
  fun `archived book keeps the repository id for reading record lookup`() {
    val archivedBook =
      ArchivedBook(
        id = "saved-book",
        title = "마션",
        creator = "앤디 위어",
        publisher = "알에이치코리아",
        year = "2026",
        coverUrl = "",
        note = "",
        totalPages = 308,
      )

    assertEquals("saved-book", archivedBook.toBookDetailArgs().id)
  }

  @Test
  fun `martian home sample keeps Pencil detail metadata`() {
    val args = TrendingBookUiModel(BookId("bk_003"), "마션", "cover-13", "").toBookDetailArgs()

    assertEquals("앤디 위어", args.creator)
    assertEquals("SF", args.category)
    assertEquals(308, args.totalPages)
  }

  @Test
  fun `reading book keeps the canonical book id instead of cover id`() {
    val args =
      ReadingBookUiModel(
        bookId = BookId("9788925588650"),
        title = "마션",
        coverId = "cover-13",
        currentPage = 80,
        totalPages = 308,
      ).toBookDetailArgs()

    assertEquals("9788925588650", args.id)
  }

  @Test
  fun `reflection date is derived from its creation time`() {
    assertEquals("2026.08.05", formatReflectionDate(1785924000000L))
  }
}
