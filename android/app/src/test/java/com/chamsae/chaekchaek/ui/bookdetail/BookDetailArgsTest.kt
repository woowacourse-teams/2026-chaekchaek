package com.chamsae.chaekchaek.ui.bookdetail

import com.chaekchaek.app.domain.book.BookId
import com.chaekchaek.app.domain.book.BookSearchResult
import com.chaekchaek.app.domain.note.NoteId
import com.chaekchaek.app.presentation.home.QuoteCardUiModel
import com.chaekchaek.app.presentation.home.ReadingBookUiModel
import com.chaekchaek.app.presentation.home.TrendingBookUiModel
import com.chamsae.chaekchaek.data.ArchivedBook
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
  fun `home book keeps server identifiers for detail loading`() {
    val args =
      TrendingBookUiModel(
        bookId = BookId("42"),
        title = "마션",
        coverId = "cover-13",
        statsLabel = "",
        isbn13 = "9788925568683",
      ).toBookDetailArgs()

    assertEquals("9788925568683", args.isbn13)
    assertEquals(42L, args.bookId)
  }

  @Test
  fun `latest review keeps server identifiers for detail loading`() {
    val args =
      QuoteCardUiModel(
        noteId = NoteId("review-1"),
        bookId = BookId("7"),
        isbn13 = "9780000000007",
        bookTitle = "역병",
        coverId = "cover-7",
        authorLabel = "독자",
        quoteText = "오래 멈춰 읽었다.",
        replyLabel = "답글 2",
      ).toBookDetailArgs()

    assertEquals("7", args.id)
    assertEquals("9780000000007", args.isbn13)
    assertEquals(7L, args.bookId)
  }

  @Test
  fun `reading book uses book id and isbn for detail loading`() {
    val args =
      ReadingBookUiModel(
        bookId = BookId("7"),
        isbn13 = "9780000000007",
        title = "역병",
        coverId = "cover-7",
        currentPage = 132,
        totalPages = 320,
      ).toBookDetailArgs()

    assertEquals("7", args.id)
    assertEquals("9780000000007", args.isbn13)
    assertEquals(7L, args.bookId)
  }
}
