package com.chaekchaek.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
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
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class BookDetailRemoteRepositoryTest {
  @Test
  fun `별점 단독 저장 응답은 독서 상태와 페이지가 null이어도 파싱한다`() = runTest {
    val client = testClient(MockEngine { request ->
      assertEquals(HttpMethod.Put, request.method)
      assertEquals("/api/v1/library/10/rating", request.url.encodedPath)
      respond("""{"bookId":10,"status":null,"currentPage":null,"rating":4.5,"readingUpdatedAt":null}""",
        headers = headersOf(HttpHeaders.ContentType, "application/json"))
    })
    try {
      val result = BookDetailRemoteRepository(client).rate(10, 4.5, "member")
      assertEquals(4.5, result.rating)
      assertNull(result.status)
      assertNull(result.currentPage)
    } finally { client.close() }
  }

  @Test
  fun `상세 별점과 책장 등록 여부는 독서 기록과 독립적으로 보존한다`() = runTest {
    for (membership in listOf("false", "true", "null")) {
      val client = testClient(MockEngine {
        respond("""{"bookId":10,"isbn13":"9788936433598","title":"책","authors":[],"translators":[],"publisher":"출판사","category":"소설","coverImageUrl":"","myRecord":{"status":null,"currentPage":null,"myRating":4.5},"isRegisteredInMyLibrary":$membership}""",
          headers = headersOf(HttpHeaders.ContentType, "application/json"))
      })
      try {
        val detail = BookDetailRemoteRepository(client).detail("9788936433598")
        assertEquals(4.5, detail.myRating)
        assertNull(detail.myRecord)
        assertEquals(membership == "true", detail.isRegisteredInMyLibrary)
      } finally { client.close() }
    }
  }

  @Test
  fun `이미 저장된 책의 추가 충돌은 회원 상세에서 기존 기록을 복원한다`() = runTest {
    val requests = mutableListOf<Triple<HttpMethod, String, String?>>()
    val client = testClient(MockEngine { request ->
      requests += Triple(request.method, request.url.encodedPath, request.headers[HttpHeaders.Authorization])
      when (request.method) {
        HttpMethod.Post -> respond(
          """{"status":409,"code":"LIBRARY_ITEM_ALREADY_EXISTS","detail":"이미 서재에 등록된 책입니다."}""",
          HttpStatusCode.Conflict,
          headersOf(HttpHeaders.ContentType, "application/problem+json"),
        )
        else -> respond(
          """{"bookId":42,"isbn13":"9780000000042","title":"책","authors":[],"translators":[],"publisher":"출판사","category":"소설","coverImageUrl":"","myRecord":{"status":"FINISHED","currentPage":308,"myRating":4.5}}""",
          headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
      }
    })
    try {
      val repository = BookDetailRemoteRepository(client, ApiConfiguration("https://api.chaekchaek.com"))

      val record = repository.addToLibrary("9780000000042", 308, "member")

      assertEquals(LibraryRecord("FINISHED", 308, 4.5, 42), record)
      assertEquals(
        listOf<Triple<HttpMethod, String, String?>>(
          Triple(HttpMethod.Post, "/api/v1/library", "Bearer member"),
          Triple(HttpMethod.Get, "/api/v1/books/by-isbn/9780000000042", "Bearer member"),
        ),
        requests,
      )
    } finally {
      client.close()
    }
  }

  @Test
  fun `다른 충돌과 확인할 수 없는 오류는 상세 조회 없이 실패로 전달한다`() = runTest {
    val errors = listOf(
      HttpStatusCode.Conflict to """{"code":"TOTAL_PAGES_CONFLICT"}""",
      HttpStatusCode.Conflict to """{"code":"UNKNOWN_CONFLICT"}""",
      HttpStatusCode.Conflict to """{"detail":"충돌"}""",
      HttpStatusCode.Conflict to "not-json",
      HttpStatusCode.BadRequest to """{"code":"LIBRARY_ITEM_ALREADY_EXISTS"}""",
    )
    for ((status, body) in errors) {
      val requests = mutableListOf<HttpMethod>()
      val client = testClient(MockEngine { request ->
        requests += request.method
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/problem+json"))
      })
      try {
        val repository = BookDetailRemoteRepository(client)

        val error = assertFailsWith<ClientRequestException> {
          repository.addToLibrary("9780000000042", 308, "member")
        }

        assertEquals(status, error.response.status)
        assertEquals(listOf(HttpMethod.Post), requests)
      } finally {
        client.close()
      }
    }
  }

  @Test
  fun `중복 충돌 후 상세에 저장 기록이 없으면 성공으로 확정하지 않는다`() = runTest {
    val client = testClient(MockEngine { request ->
      if (request.method == HttpMethod.Post) {
        respond(
          """{"code":"LIBRARY_ITEM_ALREADY_EXISTS"}""",
          HttpStatusCode.Conflict,
          headersOf(HttpHeaders.ContentType, "application/problem+json"),
        )
      } else {
        respond(
          """{"bookId":42,"isbn13":"9780000000042","title":"책","authors":[],"translators":[],"publisher":"출판사","category":"소설","coverImageUrl":"","myRecord":null}""",
          headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
      }
    })
    try {
      val repository = BookDetailRemoteRepository(client)

      val error = assertFailsWith<ClientRequestException> {
        repository.addToLibrary("9780000000042", 308, "member")
      }

      assertEquals(HttpStatusCode.Conflict, error.response.status)
    } finally {
      client.close()
    }
  }

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
      apiConfiguration = ApiConfiguration("https://api.chaekchaek.com"),
      client = HttpClient(engine) {
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
      apiConfiguration = ApiConfiguration("https://api.chaekchaek.com"),
      client = HttpClient(engine) {
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
      apiConfiguration = ApiConfiguration("https://api.chaekchaek.com"),
      client = HttpClient(engine) {
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
  fun `ISBN 감상 작성은 미등록 도서 등록 결과의 감상을 반환한다`() = runTest {
    var requestedPath = ""
    val engine = MockEngine { request ->
      requestedPath = request.url.encodedPath
      respond(
        content = """{"bookId":42,"review":{"reviewId":7,"content":"감상","createdAt":"2026-08-27T00:00:00Z","author":{"displayName":"회원","anonymous":false,"mine":true,"actorType":"MEMBER"},"replyCount":0,"likeCount":0,"deleted":false}}""",
        status = HttpStatusCode.Created,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
      )
    }
    val repository = BookDetailRemoteRepository(
      apiConfiguration = ApiConfiguration("https://api.chaekchaek.com"),
      client = HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
      },
    )

    val review = repository.createReviewByIsbn(
      isbn13 = "9780000000042",
      request = ReviewCreateRequest("감상"),
      credential = WriteCredential.Member("member"),
    )

    assertEquals("/api/v1/books/by-isbn/9780000000042/reviews", requestedPath)
    assertEquals(7, review.reviewId)
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
      apiConfiguration = ApiConfiguration("https://api.chaekchaek.com"),
      client = HttpClient(engine) {
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
    val repository = BookDetailRemoteRepository(testClient(engine), ApiConfiguration("https://api.chaekchaek.com"))
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
  fun `감상 본문 수정은 회원과 게스트 모두 숨긴 읽기 메타데이터를 생략한다`() = runTest {
    for (credential in listOf(WriteCredential.Member("member"), WriteCredential.Guest("guest-token"))) {
      val actorType = if (credential is WriteCredential.Member) "MEMBER" else "GUEST"
      val client = testClient(MockEngine { request ->
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("/api/v1/reviews/7", request.url.encodedPath)
        assertEquals(credential.headerValue, request.headers[credential.headerName])
        assertEquals(
          Json.parseToJsonElement("""{"content":"수정 감상","quote":null,"isSpoiler":true}"""),
          Json.parseToJsonElement((request.body as TextContent).text),
        )
        respond(
          """{"reviewId":7,"content":"수정 감상","chapter":"기존 장","currentPage":80,"createdAt":"2026-10-21T00:00:00Z","author":{"displayName":"작성자","anonymous":false,"mine":true,"actorType":"$actorType"},"replyCount":0,"likeCount":0,"isSpoiler":true,"deleted":false}""",
          headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
      })
      try {
        val repository = BookDetailRemoteRepository(client, ApiConfiguration("https://api.chaekchaek.com"))
        val review = repository.updateReviewContent(
          7,
          ReviewCreateRequest("수정 감상", quote = null, chapter = "보내면 안 되는 장", currentPage = 999, totalPages = 100, isSpoiler = true),
          credential,
        )
        assertEquals("기존 장", review.chapter)
        assertEquals(80, review.currentPage)
      } finally {
        client.close()
      }
    }
  }

  @Test
  fun `기존 회원 감상 수정은 읽기 메타데이터와 선택 입력 삭제를 계속 전송한다`() = runTest {
    val client = testClient(MockEngine { request ->
      assertEquals(HttpMethod.Patch, request.method)
      assertEquals("/api/v1/reviews/7", request.url.encodedPath)
      assertEquals("Bearer member", request.headers[HttpHeaders.Authorization])
      assertEquals(
        Json.parseToJsonElement("""{"content":"수정 감상","quote":null,"chapter":null,"currentPage":80,"totalPages":100,"isSpoiler":false}"""),
        Json.parseToJsonElement((request.body as TextContent).text),
      )
      respond(
        """{"reviewId":7,"content":"수정 감상","currentPage":80,"createdAt":"2026-10-21T00:00:00Z","author":{"displayName":"작성자","anonymous":false,"mine":true,"actorType":"MEMBER"},"replyCount":0,"likeCount":0,"deleted":false}""",
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
      )
    })
    try {
      val repository = BookDetailRemoteRepository(client, ApiConfiguration("https://api.chaekchaek.com"))
      repository.updateReview(
        7,
        ReviewCreateRequest("수정 감상", quote = null, chapter = null, currentPage = 80, totalPages = 100),
        WriteCredential.Member("member"),
      )
    } finally {
      client.close()
    }
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
