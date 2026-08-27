package com.chaekchaek.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class BookDetailRemoteRepositoryTest {
  @Test
  fun `상세와 감상 응답을 화면 모델로 변환한다`() {
    val detail = BookDetailDto(
      bookId = 42,
      isbn13 = "9788925568683",
      title = "마션",
      authors = listOf("앤디 위어"),
      translators = listOf("박아람"),
      publisher = "알에이치코리아",
      category = "국내도서>소설>과학소설",
      coverImageUrl = "https://example.com/martian.jpg",
      totalPages = 308,
      myRecord = BookDetailRecordDto(status = "READING", currentPage = 120, myRating = 4.5),
    ).toBookDetail()
    val reviews = ReviewPageDto(
      totalCount = 1,
      nextPage = 2,
      items = listOf(
        ReviewDto(
          reviewId = 7,
          content = "재미있다",
          createdAt = "2026-08-19T00:00:00Z",
          author = ReviewAuthorDto(
            displayName = "참새 1204",
            anonymous = true,
            mine = true,
            actorType = "GUEST",
            profileImageUrl = "https://example.com/reviewer.jpg",
          ),
          replyCount = 2,
          likeCount = 3,
          likedByMe = true,
          isSpoiler = true,
          recentReplies = listOf(
            ReviewReplyDto(
              replyId = 8,
              content = "맞아요",
              author = ReviewAuthorDto(
                displayName = "참새 0821",
                anonymous = false,
                mine = false,
                actorType = "MEMBER",
                profileImageUrl = "https://example.com/replier.jpg",
              ),
              likeCount = 1,
              likedByMe = true,
              deleted = false,
            ),
          ),
          deleted = false,
        ),
      ),
    ).toReviewPage()

    assertEquals("과학소설", detail.category)
    assertEquals(42, detail.bookId)
    assertEquals(120, detail.myRecord?.currentPage)
    assertEquals(4.5, detail.myRecord?.rating)
    assertEquals("참새 1204", reviews.items.single().authorName)
    assertEquals("https://example.com/reviewer.jpg", reviews.items.single().authorProfileImageUrl)
    assertEquals(true, reviews.items.single().isSpoiler)
    assertEquals(true, reviews.items.single().likedByMe)
    assertEquals(true, reviews.items.single().writtenByMe)
    assertEquals(false, reviews.items.single().deleted)
    assertEquals("맞아요", reviews.items.single().recentReplies.single().content)
    assertEquals("참새 0821", reviews.items.single().recentReplies.single().authorName)
    assertEquals("https://example.com/replier.jpg", reviews.items.single().recentReplies.single().authorProfileImageUrl)
    assertEquals(true, reviews.items.single().recentReplies.single().likedByMe)
    assertEquals(false, reviews.items.single().recentReplies.single().writtenByMe)
    assertEquals(false, reviews.items.single().recentReplies.single().deleted)
    assertEquals(2, reviews.nextPage)
  }

  @Test
  fun `답글 조회와 감상 답글 반응 요청은 서버 계약의 경로와 메서드를 사용한다`() = runTest {
    val requests = mutableListOf<Pair<HttpMethod, String>>()
    val engine = MockEngine { request ->
      requests += request.method to request.url.toString()
      when {
        request.method == HttpMethod.Get -> respond(
          content = """{"totalCount":1,"nextPage":null,"items":[{"replyId":8,"content":"맞아요","author":{"displayName":"참새 0821","anonymous":false,"mine":true,"actorType":"GUEST"},"likeCount":2,"likedByMe":true,"deleted":false}]}""",
          headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
        request.method == HttpMethod.Post -> respond(
          content = """{"likeCount":3,"likedByMe":true}""",
          status = HttpStatusCode.Created,
          headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
        else -> respond(content = "", status = HttpStatusCode.NoContent)
      }
    }
    val repository = BookDetailRemoteRepository(
      HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
      },
    )

    val replies = repository.replies(reviewId = 7, accessToken = "test-token", page = 2)
    val reviewReaction = repository.likeReview(reviewId = 7, accessToken = "test-token")
    repository.unlikeReview(reviewId = 7, accessToken = "test-token")
    val replyReaction = repository.likeReply(replyId = 8, accessToken = "test-token")
    repository.unlikeReply(replyId = 8, accessToken = "test-token")

    assertEquals(8, replies.items.single().replyId)
    assertEquals(true, replies.items.single().likedByMe)
    assertEquals(true, replies.items.single().writtenByMe)
    assertEquals(ReactionResult(3, true), reviewReaction)
    assertEquals(ReactionResult(3, true), replyReaction)
    assertEquals(
      listOf(
        HttpMethod.Get to "https://api.chaekchaek.com/api/v1/reviews/7/replies?page=2",
        HttpMethod.Post to "https://api.chaekchaek.com/api/v1/reviews/7/reactions",
        HttpMethod.Delete to "https://api.chaekchaek.com/api/v1/reviews/7/reactions",
        HttpMethod.Post to "https://api.chaekchaek.com/api/v1/replies/8/reactions",
        HttpMethod.Delete to "https://api.chaekchaek.com/api/v1/replies/8/reactions",
      ),
      requests,
    )
  }

  @Test
  fun `상세 서재 기록의 비어 있는 현재 페이지는 0으로 변환한다`() {
    val record = BookDetailRecordDto(status = "WANT_TO_READ", currentPage = null, myRating = null)
      .toLibraryRecord()

    assertEquals("WANT_TO_READ", record?.status)
    assertEquals(0, record?.currentPage)
  }

  @Test
  fun `서재 변경 응답의 도서 ID를 유지한다`() {
    val record = LibraryRecordDto(bookId = 10, status = "READING", currentPage = 0)
      .toLibraryRecord()

    assertEquals(10, record.bookId)
  }
}
