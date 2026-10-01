package com.chaekchaek.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReviewFeedRemoteRepositoryTest {
    @Test
    fun `피드 페이지와 스포일러 상태를 보존한다`() = runTest {
        var request: io.ktor.client.request.HttpRequestData? = null
        val client = HttpClient(MockEngine { captured ->
            request = captured
            respond(
                content = """{
                    "totalCount":41,
                    "nextPage":2,
                    "reviews":[{
                        "reviewId":123,
                        "content":"감상 내용",
                        "quote":"인용 문장",
                        "isSpoiler":true,
                        "createdAt":"2026-09-28T10:00:00Z",
                        "author":{"displayName":"독자","profileImageUrl":null},
                        "likeCount":7,
                        "likedByMe":true,
                        "replyCount":3,
                        "bookId":42,
                        "isbn13":"9788936433598",
                        "bookTitle":"도서 제목",
                        "bookCoverImageUrl":"https://example.com/cover.jpg"
                    }]
                }""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val page = ReviewFeedRemoteRepository(client).reviewFeed(page = 2, accessToken = "member-token")

        assertEquals(41, page.totalCount)
        assertEquals(2, page.nextPage)
        assertEquals(123, page.reviews.single().reviewId)
        assertTrue(page.reviews.single().isSpoiler)
        assertEquals("2", request?.url?.parameters?.get("page"))
        assertEquals("Bearer member-token", request?.headers?.get(HttpHeaders.Authorization))
    }
}
