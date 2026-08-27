package com.chaekchaek.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
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

    val credential = WriteCredential.Member("test-token")
    val replies = repository.replies(reviewId = 7, credential = credential, page = 2)
    val reviewReaction = repository.likeReview(reviewId = 7, credential = credential)
    repository.unlikeReview(reviewId = 7, credential = credential)
    val replyReaction = repository.likeReply(replyId = 8, credential = credential)
    repository.unlikeReply(replyId = 8, credential = credential)

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
  fun `회원과 게스트 쓰기 자격을 각각 올바른 헤더로 보낸다`() = runTest {
    val headers = mutableListOf<Pair<String?, String?>>()
    val engine = MockEngine { request ->
      headers += request.headers[HttpHeaders.Authorization] to
        request.headers[WriteCredential.GUEST_TOKEN_HEADER]
      respond(
        content = """{"likeCount":1,"likedByMe":true}""",
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
      )
    }
    val repository = BookDetailRemoteRepository(
      HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
      },
    )

    repository.likeReview(7, WriteCredential.Member("member-token"))
    repository.likeReview(7, WriteCredential.Guest("guest-token"))

    assertEquals(
      listOf("Bearer member-token" to null, null to "guest-token"),
      headers,
    )
  }

  @Test
  fun `게스트 감상 작성 본문에서는 쪽수 필드를 제외한다`() = runTest {
    val bodies = mutableListOf<String>()
    val engine = MockEngine { request ->
      bodies += (request.body as TextContent).text
      respond(
        content = """{"reviewId":7,"content":"감상","createdAt":"2026-08-27T00:00:00Z","author":{"displayName":"게스트","anonymous":false,"mine":true,"actorType":"GUEST"},"replyCount":0,"likeCount":0,"deleted":false}""",
        status = HttpStatusCode.Created,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
      )
    }
    val repository = BookDetailRemoteRepository(
      HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
      },
    )
    val request = ReviewCreateRequest("감상", currentPage = 10, totalPages = 100)

    repository.createReview(42, request, WriteCredential.Member("member"))
    repository.createReview(42, request, WriteCredential.Guest("guest"))

    assertEquals(true, bodies.first().contains("\"currentPage\":10"))
    assertEquals(true, bodies.first().contains("\"totalPages\":100"))
    assertEquals(false, bodies.last().contains("currentPage"))
    assertEquals(false, bodies.last().contains("totalPages"))
  }

  @Test
  fun `상세와 감상 답글 조회에 게스트 토큰을 보낸다`() = runTest {
    val sentGuestTokens = mutableListOf<String?>()
    val engine = MockEngine { request ->
      sentGuestTokens += request.headers[WriteCredential.GUEST_TOKEN_HEADER]
      val content = when {
        request.url.encodedPath.endsWith("/by-isbn/9780000000042") ->
          """{"bookId":42,"isbn13":"9780000000042","title":"책","authors":[],"translators":[],"publisher":"출판사","category":"소설","coverImageUrl":""}"""
        request.url.encodedPath.endsWith("/reviews/7/replies") ->
          """{"totalCount":0,"items":[]}"""
        else -> """{"totalCount":0,"items":[]}"""
      }
      respond(content, headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }
    val repository = BookDetailRemoteRepository(
      HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
      },
    )
    val guest = WriteCredential.Guest("guest-token")

    repository.detail("9780000000042", guest)
    repository.reviews(42, ReviewScope.ALL, ReviewSort.LATEST, guest)
    repository.replies(7, guest)

    assertEquals(listOf<String?>("guest-token", "guest-token", "guest-token"), sentGuestTokens)
  }

  @Test
  fun `감상과 답글 수정 삭제는 계약의 메서드와 경로를 사용한다`() = runTest {
    val requests = mutableListOf<Pair<HttpMethod, String>>()
    val patchBodies = mutableListOf<String>()
    val engine = MockEngine { request ->
      requests += request.method to request.url.toString()
      if (request.method == HttpMethod.Patch) patchBodies += (request.body as TextContent).text
      val content = if (request.url.encodedPath.contains("/replies/")) {
        """{"replyId":8,"content":"수정 답글","author":{"displayName":"게스트","anonymous":false,"mine":true,"actorType":"GUEST"},"likeCount":0,"likedByMe":false,"deleted":false}"""
      } else {
        """{"reviewId":7,"content":"수정 감상","createdAt":"2026-08-27T00:00:00Z","author":{"displayName":"게스트","anonymous":false,"mine":true,"actorType":"GUEST"},"replyCount":0,"likeCount":0,"likedByMe":false,"isSpoiler":false,"recentReplies":[],"deleted":false}"""
      }
      if (request.method == HttpMethod.Delete) {
        respond("", HttpStatusCode.NoContent)
      } else {
        respond(content, headers = headersOf(HttpHeaders.ContentType, "application/json"))
      }
    }
    val repository = BookDetailRemoteRepository(testClient(engine))
    val guest = WriteCredential.Guest("guest-token")

    repository.updateReview(
      7,
      ReviewCreateRequest("수정 감상", quote = null, chapter = null, currentPage = 10, totalPages = 100),
      guest,
    )
    repository.deleteReview(7, guest)
    repository.updateReply(8, "수정 답글", guest)
    repository.deleteReply(8, guest)

    assertEquals(
      listOf(
        HttpMethod.Patch to "https://api.chaekchaek.com/api/v1/reviews/7",
        HttpMethod.Delete to "https://api.chaekchaek.com/api/v1/reviews/7",
        HttpMethod.Patch to "https://api.chaekchaek.com/api/v1/replies/8",
        HttpMethod.Delete to "https://api.chaekchaek.com/api/v1/replies/8",
      ),
      requests,
    )
    assertEquals(true, patchBodies.first().contains("\"quote\":null"))
    assertEquals(true, patchBodies.first().contains("\"chapter\":null"))
    assertEquals(false, patchBodies.first().contains("currentPage"))
    assertEquals(false, patchBodies.first().contains("totalPages"))
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

  private fun testClient(engine: MockEngine) = HttpClient(engine) {
    expectSuccess = true
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
  }
}
