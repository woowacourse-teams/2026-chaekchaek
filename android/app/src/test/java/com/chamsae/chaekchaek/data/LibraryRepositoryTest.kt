package com.chamsae.chaekchaek.data

import com.chaekchaek.app.data.remote.RemoteLibraryBook
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryRepositoryTest {

  @Test
  fun `server library book keeps identifiers and reading state`() {
    val archived =
      RemoteLibraryBook(
        bookId = 10,
        isbn13 = "9788936433598",
        title = "채식주의자",
        coverImageUrl = "https://example.com/cover.jpg",
        authors = listOf("한강"),
        publisher = "창비",
        category = "국내도서>소설",
        publishedDate = "2007-10-30",
        totalPages = 368,
        status = "READING",
        currentPage = 100,
        readingUpdatedAt = "2026-08-14T03:30:00Z",
      ).toArchivedBook()

    assertEquals(10L, archived.bookId)
    assertEquals("9788936433598", archived.id)
    assertEquals(ReadingStatus.Reading, archived.status)
    assertEquals(100, archived.currentPage)
    assertEquals(368, archived.totalPages)
    assertEquals(1786678200000L, archived.lastRecordedAt)
  }
}
