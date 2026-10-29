package com.chaekchaek.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.*

class PublicLibraryRemoteRepositoryTest {
    @Test fun publicCollectionKeepsOwnerOrderEmptyShelvesAndCrossShelfDuplicates() = runTest {
        var captured: HttpRequestData? = null
        val item = """{"bookId":7,"isbn13":"9780000000007","title":"책","coverImageUrl":"cover","authors":["저자"],"spineImageUrl":null}"""
        val client = client("""{"memberId":42,"libraries":[
            {"libraryId":9,"name":"독자","isDefault":true,"totalCount":1,"items":[$item]},
            {"libraryId":3,"name":"빈 책장","isDefault":false,"totalCount":0,"items":[]},
            {"libraryId":5,"name":"긴 이름도 서버 원문 그대로 보존하는 책장","isDefault":false,"totalCount":1,"items":[$item]}]}""") { captured = it }
        val result = PublicLibraryRemoteRepository(client).library(42)
        assertEquals("/api/v1/libraries", captured?.url?.encodedPath)
        assertEquals("42", captured?.url?.parameters?.get("memberId"))
        assertNull(captured?.headers?.get(HttpHeaders.Authorization))
        assertNull(captured?.headers?.get(WriteCredential.GUEST_TOKEN_HEADER))
        assertEquals(listOf(9L, 3L, 5L), result.libraries.map { it.libraryId })
        assertEquals("독자", result.libraries.first().name)
        assertTrue(result.libraries[1].items.isEmpty())
        assertEquals(7L, result.libraries.last().items.single().bookId)
        assertNull(result.libraries.first().items.single().spineImageUrl)
        assertEquals("cover", result.libraries.first().items.single().coverImageUrl)
        assertEquals(listOf("저자"), result.libraries.first().items.single().authors)
    }
    @Test fun detailReadsAllLegacyBooksAndOwnerReadingStateWithoutLogin() = runTest {
        val books = (31 downTo 1).joinToString(",") { id ->
            """{"bookId":$id,"isbn13":"9780000000001","title":"책 $id","coverImageUrl":"cover","spineImageUrl":null,"authors":[],"totalPages":null,"status":"READING","currentPage":23,"readingUpdatedAt":"2026-10-23T00:00:00Z"}"""
        }
        var captured: HttpRequestData? = null
        val repo = PublicLibraryRemoteRepository(client("""{"libraryId":9,"memberId":42,"name":"독자","isDefault":true,"totalCount":31,"items":[$books]}""") { captured = it })
        val result = repo.bookshelf(9)
        assertEquals("/api/v1/libraries/9", captured?.url?.encodedPath)
        assertNull(captured?.headers?.get(HttpHeaders.Authorization))
        assertEquals(31, result.items.size)
        assertEquals((31L downTo 1L).toList(), result.items.map { it.bookId })
        assertEquals("READING", result.items.first().status)
        assertEquals(23, result.items.first().currentPage)
        assertNull(result.items.first().totalPages)
        assertTrue(result.items.first().authors.isEmpty())
    }
    @Test fun responseForAnotherOwnerIsRejected() = runTest {
        val repo = PublicLibraryRemoteRepository(client("""{"memberId":99,"libraries":[]}"""))
        assertFailsWith<IllegalStateException> { repo.library(42) }
    }
    @Test fun absentLibraryIsNotMisreportedAsAnEmptyLibrary() = runTest {
        val repo = PublicLibraryRemoteRepository(client("{}", status = HttpStatusCode.NotFound))
        assertFailsWith<io.ktor.client.plugins.ClientRequestException> { repo.library(42) }
    }
    private fun client(body: String, status: HttpStatusCode = HttpStatusCode.OK, capture: (HttpRequestData) -> Unit = {}) =
        HttpClient(MockEngine { request ->
            capture(request)
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }) { expectSuccess = true; install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
}
