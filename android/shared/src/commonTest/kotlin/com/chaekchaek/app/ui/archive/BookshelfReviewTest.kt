package com.chaekchaek.app.ui.archive

import com.chaekchaek.app.data.remote.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class BookshelfReviewTest {
    @Test fun `소유자 책 순서대로 최신 내 감상만 선택하고 없는 감상은 비운다`() = runTest {
        val paths = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            assertEquals("MINE", request.url.parameters["feed"])
            assertEquals("LATEST", request.url.parameters["sort"])
            assertEquals("Bearer member", request.headers[HttpHeaders.Authorization])
            paths += request.url.encodedPath + "?page=" + request.url.parameters["page"]
            val bookId = request.url.encodedPath.split("/")[4].toLong()
            val page = request.url.parameters["page"]
            val body = when {
                bookId == 2L -> """{"totalCount":0,"items":[]}"""
                page == "1" -> """{"totalCount":3,"nextPage":2,"items":[${review(8, true)}]}"""
                else -> """{"totalCount":3,"items":[${review(7, false)},${review(6, false)}]}"""
            }
            respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json() } }
        try {
            val detail = BookshelfDetail(1, 9, "책장", true, 2, listOf(
                BookshelfBook(3, "isbn3", "책3", ""), BookshelfBook(2, "isbn2", "책2", ""),
            ))
            val result = latestShelfReviews(detail, "member", BookDetailRemoteRepository(client))
            assertEquals(listOf(3L, 2L), result.keys.toList())
            assertEquals(7L, result[3]?.reviewId)
            assertNull(result[2])
            assertEquals(listOf("/api/v1/books/3/reviews?page=1", "/api/v1/books/3/reviews?page=2", "/api/v1/books/2/reviews?page=1"), paths)
        } finally { client.close() }
    }

    private fun review(id: Int, deleted: Boolean) = """{"reviewId":$id,"content":"감상$id","createdAt":"2026-10-23T00:00:00Z","author":{"displayName":"나","anonymous":false,"mine":true,"actorType":"MEMBER"},"replyCount":0,"likeCount":0,"deleted":$deleted}"""
}
