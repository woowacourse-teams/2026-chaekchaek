package com.chaekchaek.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
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
  fun `member settings use authenticated member endpoints`() = runTest {
    val requests = mutableListOf<HttpRequestData>()
    val client = HttpClient(MockEngine { request ->
      requests += request
      respond(
        content = """{"memberId":9,"nickname":"책책이","displayAnonymous":false}""",
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
      )
    }) {
      expectSuccess = true
      install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
    val repository = LibraryRemoteRepository(client)

    assertEquals(9L, repository.getMember("access-token").memberId)
    repository.updateNickname("새 이름", "access-token")
    repository.updateAnonymity(true, "access-token")

    assertEquals(
      listOf(
        HttpMethod.Get to "/api/v1/members/me",
        HttpMethod.Patch to "/api/v1/members/me/nickname",
        HttpMethod.Patch to "/api/v1/members/me/anonymity",
      ),
      requests.map { it.method to it.url.encodedPath },
    )
    assertEquals(List(3) { "Bearer access-token" }, requests.map { it.headers[HttpHeaders.Authorization] })
    assertEquals(Json.parseToJsonElement("""{"nickname":"새 이름"}"""), requests[1].jsonBody())
    assertEquals(Json.parseToJsonElement("""{"displayAnonymous":true}"""), requests[2].jsonBody())
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

  private fun HttpRequestData.jsonBody() =
    Json.parseToJsonElement((body as OutgoingContent.ByteArrayContent).bytes().decodeToString())
}
