package com.chaekchaek.app.data.remote

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
import kotlin.test.assertNull
import com.chaekchaek.app.domain.feed.FeedSection

class FutureBookCompatibilityTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun `홈 인기 책과 최근 감상은 ID null과 누락을 제외한다`() {
    val popular = json.decodeFromString<PopularBooksResponseDto>("""{"books":[
      {"bookId":null,$POPULAR},{$POPULAR},{"bookId":42,$POPULAR}]}""")
    val latest = json.decodeFromString<LatestReviewsResponseDto>("""{"reviews":[
      {"bookId":null,$LATEST},{$LATEST},{"bookId":42,$LATEST}]}""")
    val feed = popular.toHomeFeed(latest)
    val trending = feed.visibleSections().filterIsInstance<FeedSection.TrendingBooks>().single()
    val quotes = feed.visibleSections().filterIsInstance<FeedSection.RecentQuotes>().single()
    assertEquals(listOf("42"), trending.books.map { it.bookId.value })
    assertEquals(1, trending.totalCount)
    assertEquals(listOf("42"), quotes.cards.map { it.bookId.value })
    val reading = json.decodeFromString<ReadingBookDto>("""{"bookId":null,"isbn13":"9780000000042","title":"책","coverImageUrl":"","currentPage":0}""")
    assertNull(popular.toHomeFeed(readingBook = reading).readingBook)
  }

  @Test
  fun `피드는 ID 없는 감상을 제외하고 서버의 다음 페이지를 보존한다`() {
    val page = json.decodeFromString<ReviewFeedPageDto>("""{"reviews":[
      {"bookId":null,$REVIEW},{$REVIEW},{"bookId":42,$REVIEW}],"totalCount":3,"nextPage":2}""")
      .toReviewFeedPage()
    assertEquals(listOf("42"), page.reviews.map { it.bookId.value })
    assertEquals(2, page.nextPage)
    assertEquals(3, page.totalCount)
  }

  @Test
  fun `내 서재는 ID 없는 항목을 건너뛰고 뒤 페이지의 정상 도서를 가져온다`() = runTest {
    val pages = mutableListOf<String?>()
    val client = HttpClient(MockEngine { request ->
      val page = request.url.parameters["page"]
      pages += page
      respond(if (page == "1") """{"nextPage":2,"items":[{"bookId":null,$LIBRARY},{$LIBRARY}]}"""
        else """{"nextPage":null,"items":[{"bookId":42,$LIBRARY}]}""",
        headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }) { install(ContentNegotiation) { json(json) } }
    try {
      assertEquals(listOf(42L), LibraryRemoteRepository(client).getAll("member").map { it.bookId })
      assertEquals(listOf<String?>("1", "2"), pages)
    } finally { client.close() }
  }

  @Test
  fun `별점 비교도 ID 없는 책을 표시하지 않는다`() = runTest {
    val client = HttpClient(MockEngine {
      respond("""{"lower":{"bookId":null,$RATING},"current":{$RATING},"higher":{"bookId":42,$RATING}}""",
        headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }) { install(ContentNegotiation) { json(json) } }
    try {
      val result = BookDetailRemoteRepository(client).ratingComparison("9780000000042", 4.0, "member")
      assertNull(result.lower)
      assertNull(result.current)
      assertEquals(42L, result.higher?.bookId)
    } finally { client.close() }
  }

  private companion object {
    const val POPULAR = "\"title\":\"책\",\"coverImageUrl\":\"\",\"reviewCount\":1,\"replyCount\":0"
    const val LATEST = "\"content\":\"감상\",\"createdAt\":\"2026-10-23T00:00:00Z\",\"author\":{\"displayName\":\"독자\"},\"replyCount\":0,\"bookTitle\":\"책\",\"bookCoverImageUrl\":\"\""
    const val REVIEW = "\"reviewId\":7,\"content\":\"감상\",\"isSpoiler\":false,\"createdAt\":\"2026-10-23T00:00:00Z\",\"author\":{\"displayName\":\"독자\"},\"likeCount\":0,\"likedByMe\":false,\"replyCount\":0,\"isbn13\":\"9780000000042\",\"bookTitle\":\"책\",\"bookCoverImageUrl\":\"\""
    const val LIBRARY = "\"isbn13\":\"9780000000042\",\"title\":\"책\",\"coverImageUrl\":\"\",\"authors\":[],\"publisher\":\"출판사\",\"category\":\"소설\",\"status\":\"READING\",\"currentPage\":0,\"readingUpdatedAt\":\"2026-10-23T00:00:00Z\""
    const val RATING = "\"title\":\"책\",\"myRating\":4.0,\"ratingUpdatedAt\":\"2026-10-23T00:00:00Z\""
  }
}
