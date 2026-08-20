package com.chamsae.chaekchaek.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryRegistrationTest {
  @Test
  fun `remote registration sends isbn pages and access token`() =
    runTest {
      var request: Triple<String, Int?, String>? = null
      val book = book(id = "9780000000001", totalPages = 320)

      val registered = registerRemotely(book, "access-token") { isbn13, totalPages, accessToken ->
        request = Triple(isbn13, totalPages, accessToken)
      }

      assertTrue(registered)
      assertEquals(Triple("9780000000001", 320, "access-token"), request)
    }

  @Test
  fun `remote registration rejects a local fallback id`() =
    runTest {
      var called = false

      val registered = registerRemotely(book(id = "title|author"), "access-token") { _, _, _ -> called = true }

      assertFalse(registered)
      assertFalse(called)
    }

  private fun book(id: String, totalPages: Int = 0) =
    ArchivedBook(
      id = id,
      title = "책",
      creator = "저자",
      publisher = "출판사",
      year = "2026",
      coverUrl = "",
      note = "",
      totalPages = totalPages,
    )
}
