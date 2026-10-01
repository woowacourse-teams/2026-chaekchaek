package com.chaekchaek.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class LibraryRemoteRepositoryTest {

  @Test
  fun `bulk book ids are unique and limited to ten per request`() {
    val chunks = chunkLibraryBookIds((1L..11L).toList() + 1L)

    assertEquals(listOf((1L..10L).toList(), listOf(11L)), chunks)
  }

  @Test
  fun `bulk delete propagates server errors`() = runTest {
    val client = HttpClient(MockEngine {
      respond("실패", HttpStatusCode.InternalServerError)
    }) {
      expectSuccess = true
      install(ContentNegotiation) { json() }
    }

    assertFailsWith<ServerResponseException> {
      LibraryRemoteRepository(client).bulkDelete(listOf(1L), "access-token")
    }
  }

  @Test
  fun `library keeps a spine image supplied by the server`() = runTest {
    val repository = LibraryRemoteRepository(libraryClient(""""spineImageUrl":"https://image.yes24.com/goods/1/side","""))

    val book = repository.getAll("access-token").single()

    assertEquals("https://image.yes24.com/goods/1/side", book.spineImageUrl)
  }

  @Test
  fun `library accepts production responses without a spine image field`() = runTest {
    val repository = LibraryRemoteRepository(libraryClient())

    val book = repository.getAll("access-token").single()

    assertNull(book.spineImageUrl)
  }

  private fun libraryClient(optionalImageFields: String = "") = HttpClient(MockEngine {
    respond(
      """{"nextPage":null,"items":[{"bookId":1,"isbn13":"9780000000001","title":"책","coverImageUrl":"cover",$optionalImageFields"authors":[],"publisher":"출판사","category":"소설","publishedDate":"2026-01-01","totalPages":100,"status":"READING","currentPage":10,"readingUpdatedAt":"2026-01-01T00:00:00Z"}]}""",
      headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )
  }) {
    install(ContentNegotiation) { json() }
  }
}
