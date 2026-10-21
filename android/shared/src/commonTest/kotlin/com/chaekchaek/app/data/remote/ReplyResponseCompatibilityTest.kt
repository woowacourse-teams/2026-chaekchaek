package com.chaekchaek.app.data.remote

import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.domain.feed.FeedSection
import com.chaekchaek.app.ui.bookdetail.loadAllReplies
import com.chaekchaek.app.ui.bookdetail.replyDisplay
import com.chaekchaek.app.ui.bookdetail.reviewDisplay
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReplyResponseCompatibilityTest {
    @Test
    fun replyListCreateAndUpdateAcceptBothContracts() = runTest {
        for (deleted in listOf<Boolean?>(null, false, true)) {
            val client = client { path ->
                if (path.endsWith("/replies")) page(reply(deleted)) else reply(deleted)
            }
            try {
                val repository = BookDetailRemoteRepository(client)
                assertEquals(deleted ?: false, repository.replies(7).items.single().deleted)
            } finally { client.close() }
            val writeClient = client { reply(deleted) }
            try {
                val repository = BookDetailRemoteRepository(writeClient)
                assertEquals(deleted ?: false, repository.createReply(7, "reply", credential).deleted)
                assertEquals(deleted ?: false, repository.updateReply(8, "reply", credential).deleted)
            } finally { writeClient.close() }
        }
    }

    @Test
    fun everyReviewResponseAcceptsNestedRepliesWithEitherContract() = runTest {
        for (deleted in listOf<Boolean?>(null, false, true)) {
            val client = client { path ->
                if (path.contains("by-isbn")) """{"bookId":42,"review":${review(deleted)}}"""
                else review(deleted)
            }
            try {
                val repository = BookDetailRemoteRepository(client)
                val request = ReviewCreateRequest(content = "review")
                val results = listOf(
                    repository.createReview(42, request, credential),
                    repository.createReviewByIsbn("9780000000042", request, credential),
                    repository.updateReview(7, request, credential),
                )
                for (result in results) {
                    assertFalse(result.deleted)
                    assertEquals(1, result.replyCount)
                    assertEquals(deleted ?: false, result.recentReplies.single().deleted)
                }
            } finally { client.close() }
            val listClient = client { page(review(deleted)) }
            try {
                val result = BookDetailRemoteRepository(listClient).reviews(42, ReviewScope.ALL, ReviewSort.LATEST)
                assertEquals(deleted ?: false, result.items.single().recentReplies.single().deleted)
                assertEquals(1, result.totalCount)
            } finally { listClient.close() }
        }
    }

    @Test
    fun reviewDeletionRemainsRequiredAndDeletedReviewKeepsRepliesAndCount() = runTest {
        assertFailsWith<SerializationException> {
            Json.decodeFromString<ReviewDto>(review(null).replace(",\"deleted\":false", ""))
        }
        val client = client { page(review(null).replace("\"deleted\":false", "\"deleted\":true")) }
        try {
            val result = BookDetailRemoteRepository(client).reviews(42, ReviewScope.ALL, ReviewSort.LATEST)
            val display = reviewDisplay(result.items, result.totalCount, hasMore = false)
            assertEquals("삭제된 감상입니다.", display.reviews.single().content)
            assertEquals(1, display.reviews.single().recentReplies.size)
            assertEquals(1, display.count)
        } finally { client.close() }
    }

    @Test
    fun activeCountsPreviewsAndNextPageAgreeWithNewContract() = runTest {
        val pages = mutableListOf<Int>()
        val client = HttpClient(MockEngine { request ->
            val page = request.url.parameters["page"]!!.toInt().also(pages::add)
            respond(
                if (page == 1) """{"totalCount":2,"nextPage":2,"items":[${reply(null)}]}"""
                else """{"totalCount":2,"items":[${reply(null).replace("\"replyId\":8", "\"replyId\":9")}]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        try {
            val repository = BookDetailRemoteRepository(client)
            val first = repository.replies(7)
            assertTrue(replyDisplay(first.items, first.totalCount).hasMore)
            pages.clear()
            val all = loadAllReplies { repository.replies(7, page = it) }
            assertEquals(listOf(1, 2), pages)
            assertEquals(listOf(8L, 9L), all.map { it.replyId })
            assertEquals(2, replyDisplay(all, 2).replies.size)
            assertFalse(replyDisplay(all, 2).hasMore)
            assertFalse(replyDisplay(emptyList(), 0).hasMore)
        } finally { client.close() }
    }

    @Test
    fun homeEnrichmentSurvivesNestedReplyWithoutDeleted() = runTest {
        val client = client { path ->
            when (path) {
                "/api/v1/home/popular-books" -> """{"books":[]}"""
                "/api/v1/home/latest-reviews" -> """{"reviews":[{"content":"review","createdAt":"2026-10-21T00:00:00Z","author":{"displayName":"reader"},"replyCount":0,"bookId":42,"bookTitle":"book","bookCoverImageUrl":""}]}"""
                else -> page(review(null))
            }
        }
        try {
            val card = PopularBooksRemoteRepository(client).homeFeed().visibleSections()
                .filterIsInstance<FeedSection.RecentQuotes>().single().cards.single()
            assertEquals(7L, card.reviewId)
            assertEquals(1, card.replyCount)
            assertEquals("quote", card.quote)
        } finally { client.close() }
    }

    @Test
    fun feedReplyWriteAcceptsResponseWithoutDeleted() = runTest {
        var calls = 0
        val client = client { calls++; reply(null) }
        try {
            val auth = AuthPlatformCallbacks({ it(null, null) }, { null }, {}, {})
            RemoteFeedReviewActions(BookDetailRemoteRepository(client), auth)
                .createReply(7, "reply", "test-member")
            assertEquals(1, calls)
        } finally { client.close() }
    }

    private fun client(body: (String) -> String) = HttpClient(MockEngine { request ->
        respond(body(request.url.encodedPath), headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }

    private fun page(item: String) = """{"totalCount":1,"items":[$item]}"""

    private fun reply(deleted: Boolean?) =
        """{"replyId":8,"content":"reply","author":$author,"likeCount":0${deleted?.let { ",\"deleted\":$it" }.orEmpty()}}"""

    private fun review(replyDeleted: Boolean?) =
        """{"reviewId":7,"content":"review","quote":"quote","createdAt":"2026-10-21T00:00:00Z","author":$author,"replyCount":1,"likeCount":0,"recentReplies":[${reply(replyDeleted)}],"deleted":false}"""

    private val author = """{"displayName":"reader","anonymous":false,"mine":true,"actorType":"MEMBER"}"""
    private val credential = WriteCredential.Member("test-member")
}
