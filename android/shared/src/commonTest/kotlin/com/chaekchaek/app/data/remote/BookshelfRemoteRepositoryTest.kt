package com.chaekchaek.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.*
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

class BookshelfRemoteRepositoryTest {
    @Test fun `읽기는 공개 계약을 쓰고 쓰기는 지정 책장과 전체 순서에 인증한다`() = runTest {
        val calls = mutableListOf<Pair<String, String>>()
        val bodies = mutableListOf<JsonElement>()
        val client = HttpClient(MockEngine { request ->
            calls += request.method.value to request.url.encodedPath
            if (request.method == HttpMethod.Get) assertNull(request.headers[HttpHeaders.Authorization])
            else {
                assertEquals("Bearer member", request.headers[HttpHeaders.Authorization])
                (request.body as? TextContent)?.let { bodies += Json.parseToJsonElement(it.text) }
            }
            val body = when {
                request.method == HttpMethod.Get && request.url.encodedPath.endsWith("/libraries") -> {
                    assertEquals("9", request.url.parameters["memberId"])
                    """{"memberId":9,"libraries":[{"libraryId":2,"name":"기존의 긴 책장 이름","isDefault":true,"totalCount":35,"items":[]}]}"""
                }
                request.method == HttpMethod.Get -> """{"libraryId":2,"memberId":9,"name":"책장","isDefault":true,"totalCount":1,"items":[{"bookId":42,"isbn13":"isbn","title":"책","coverImageUrl":"cover","authors":[],"totalPages":null,"status":"READING","currentPage":23}]}"""
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("/2") -> """{"bookId":42,"status":"FINISHED","currentPage":100,"rating":4.5}"""
                request.method == HttpMethod.Post -> """{"libraryId":3,"name":"새 책장","isDefault":false,"totalCount":0,"items":[]}"""
                else -> ""
            }
            respond(body, if (body.isEmpty()) HttpStatusCode.NoContent else HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) { expectSuccess = true; install(ContentNegotiation) { json() } }
        val repo = BookshelfRemoteRepository(client)
        assertEquals(35, repo.collection(9).libraries.single().totalCount)
        val detail = repo.detail(2)
        assertTrue(detail.items.single().authors.isEmpty())
        assertNull(detail.items.single().totalPages)
        assertEquals(LibraryRecord("FINISHED", 100, 4.5, 42), repo.add(2, "isbn", null, "member"))
        assertEquals(3L, repo.create("새 책장", "member").libraryId)
        repo.rename(2, "이름", "member")
        repo.orderShelves(listOf(3, 2), "member")
        repo.orderBooks(2, (1L..35).reversed().toList(), "member")
        repo.remove(2, 42, "member")
        repo.delete(3, "member")
        assertEquals(listOf("GET" to "/api/v1/libraries", "GET" to "/api/v1/libraries/2", "POST" to "/api/v1/libraries/2", "POST" to "/api/v1/libraries", "PATCH" to "/api/v1/libraries/2", "PATCH" to "/api/v1/libraries/order", "PATCH" to "/api/v1/libraries/2/books/order", "DELETE" to "/api/v1/libraries/2/books/42", "DELETE" to "/api/v1/libraries/3"), calls)
        assertEquals("READING", bodies[0].jsonObject["status"]?.jsonPrimitive?.content)
        assertEquals(Json.parseToJsonElement("""{"libraryIds":[3,2]}"""), bodies[3])
        assertEquals(35, bodies[4].jsonObject["bookIds"]!!.jsonArray.size)
    }

    @Test fun `책장 용량 충돌을 중복 성공으로 삼지 않는다`() = runTest {
        val client = HttpClient(MockEngine { respond("""{"code":"LIBRARY_ITEM_LIMIT_EXCEEDED"}""", HttpStatusCode.Conflict, headersOf(HttpHeaders.ContentType, "application/problem+json")) }) {
            expectSuccess = true; install(ContentNegotiation) { json() }
        }
        assertFailsWith<ClientRequestException> { BookshelfRemoteRepository(client).add(2, "isbn", null, "member") }
    }
}
