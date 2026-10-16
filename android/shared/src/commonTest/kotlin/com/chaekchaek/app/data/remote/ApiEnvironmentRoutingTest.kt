package com.chaekchaek.app.data.remote

import com.chaekchaek.app.domain.book.BookSearchSort
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ApiEnvironmentRoutingTest {
    @Test
    fun `모든 API 저장소가 주입한 환경으로 요청한다`() = runTest {
        listOf("https://api.chaekchaek.com", "https://dev-api.chaekchaek.com", "http://10.0.2.2:9090", "http://localhost:9090")
            .forEach { origin ->
                val requestedUrls = mutableListOf<String>()
                val client = HttpClient(MockEngine { request ->
                    requestedUrls += request.url.toString()
                    val response = when (request.url.encodedPath) {
                        "/api/v1/books" -> """{"totalCount":0,"items":[]}"""
                        "/api/v1/home/popular-books" -> """{"books":[]}"""
                        "/api/v1/home/latest-reviews" -> """{"reviews":[]}"""
                        "/api/v1/library" -> """{"items":[]}"""
                        "/api/v1/auth/guest-token" -> """{"guestToken":"test-guest","nickname":"테스트","expiresAt":"2026-10-01T00:00:00"}"""
                        else -> "{}"
                    }
                    respond(response, headers = headersOf(HttpHeaders.ContentType, "application/json"))
                }) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                }
                try {
                    val configuration = ApiConfiguration("$origin/")
                    BookSearchRemoteRepository(client, configuration).search("책", BookSearchSort.entries.first(), 1)
                    PopularBooksRemoteRepository(client, configuration).homeFeed("test-member")
                    LibraryRemoteRepository(client, configuration).getAll("test-member")
                    MemberRemoteRepository(client, configuration).withdraw("test-member")
                    BookDetailRemoteRepository(client, configuration).unlikeReview(7, WriteCredential.Guest("test-guest"))
                    MobileAuthRemoteRepository(client, configuration).issueGuest()
                    assertEquals(8, requestedUrls.size)
                    assertEquals(listOf(
                        "$origin/api/v1/books", "$origin/api/v1/home/popular-books",
                        "$origin/api/v1/home/latest-reviews", "$origin/api/v1/library",
                        "$origin/api/v1/library", "$origin/api/v1/members/me",
                        "$origin/api/v1/reviews/7/reactions", "$origin/api/v1/auth/guest-token",
                    ), requestedUrls.map { it.substringBefore('?') })
                } finally {
                    client.close()
                }
            }
    }

    @Test
    fun `API 주소에 자격증명과 경로 또는 쿼리를 넣을 수 없다`() {
        listOf("ftp://example.com", "https://user:password@example.com", "https://example.com/api", "https://example.com?token=test", "https://example.com#fragment")
            .forEach { origin -> assertFailsWith<IllegalArgumentException> { ApiConfiguration(origin) } }
    }

    @Test
    fun `기본 설정은 선택한 플랫폼 빌드 환경을 따른다`() {
        assertEquals(platformApiBaseUrl(), ApiConfiguration.current.baseUrl)
        assertEquals(GeneratedApiEnvironment.name, ApiConfiguration.environment)
    }
}
