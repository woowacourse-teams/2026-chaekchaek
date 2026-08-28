package com.chaekchaek.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
}
