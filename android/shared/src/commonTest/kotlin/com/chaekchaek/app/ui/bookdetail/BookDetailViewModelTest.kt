package com.chaekchaek.app.ui.bookdetail

import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.auth.GuestAuth
import com.chaekchaek.app.data.remote.BookDetailRemoteRepository
import com.chaekchaek.app.data.remote.LibraryRemoteRepository
import com.chaekchaek.app.data.remote.MobileAuthRemoteRepository
import com.chaekchaek.app.data.remote.ReviewCreateRequest
import com.chaekchaek.app.data.remote.WriteCredential
import com.chaekchaek.app.domain.rating.Rating
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BookDetailViewModelTest {
  @Test
  fun `회원 전용 액션만 로그인 보류 상태로 만든다`() = runViewModelTest {
    val client = testClient(MockEngine { error("네트워크를 호출하면 안 됨") })
    val viewModel = viewModel(client, client, platform(readGuest = { null }))

    assertTrue(viewModel.requestAuthentication(BookDetailAuthenticatedAction.OpenReview))
    assertNull(viewModel.uiState.value.pendingAction)

    assertFalse(viewModel.requestAuthentication(BookDetailAuthenticatedAction.AddToLibrary))
    assertEquals(BookDetailAuthenticatedAction.AddToLibrary, viewModel.uiState.value.pendingAction)
  }

  @Test
  fun `게스트 신규 작성은 lazy 발급하고 401이면 새 토큰으로 한 번 재시도한다`() = runViewModelTest {
    var guest: GuestAuth? = null
    var issueCount = 0
    val sentGuestTokens = mutableListOf<String?>()
    val secondWriteStarted = CompletableDeferred<Unit>()
    val finishSecondWrite = CompletableDeferred<Unit>()
    val writeEngine = MockEngine { request ->
      sentGuestTokens += request.headers[WriteCredential.GUEST_TOKEN_HEADER]
      if (sentGuestTokens.size == 1) {
        respond("", HttpStatusCode.Unauthorized)
      } else {
        secondWriteStarted.complete(Unit)
        finishSecondWrite.await()
        respond(
          content = REVIEW_RESPONSE,
          status = HttpStatusCode.Created,
          headers = jsonHeaders(),
        )
      }
    }
    val authEngine = MockEngine {
      issueCount += 1
      respond(
        content = """{"guestToken":"guest-$issueCount","nickname":"게스트 $issueCount","expiresAt":"2026-09-25T09:00:00"}""",
        status = HttpStatusCode.Created,
        headers = jsonHeaders(),
      )
    }
    val viewModel = viewModel(
      testClient(writeEngine),
      testClient(authEngine),
      platform({ guest }, { guest = it }),
    )
    viewModel.open(book(), accessToken = null)
    advanceUntilIdle()

    viewModel.createReview(ReviewCreateRequest("감상", currentPage = 10, totalPages = 100))
    awaitReal { secondWriteStarted.await() }
    advanceTimeBy(API_LOADING_DELAY_MILLIS)
    runCurrent()
    assertTrue(viewModel.uiState.value.isSubmitting)
    finishSecondWrite.complete(Unit)
    awaitReal { viewModel.uiState.first { !it.isSubmitting } }
    advanceUntilIdle()

    assertEquals(listOf<String?>("guest-1", "guest-2"), sentGuestTokens)
    assertEquals(2, issueCount)
    assertEquals("guest-2", guest?.token)
    assertNull(viewModel.uiState.value.requestError)
  }

  @Test
  fun `게스트 신규 작성 재시도도 401이면 실패로 끝난다`() = runViewModelTest {
    var guest: GuestAuth? = guest("old")
    var writeCount = 0
    var issueCount = 0
    val writeEngine = MockEngine {
      writeCount += 1
      respond("", HttpStatusCode.Unauthorized)
    }
    val authEngine = MockEngine {
      issueCount += 1
      respond(
        content = """{"guestToken":"new","nickname":"새 게스트","expiresAt":"2026-09-25T09:00:00"}""",
        status = HttpStatusCode.Created,
        headers = jsonHeaders(),
      )
    }
    val viewModel = viewModel(
      testClient(writeEngine),
      testClient(authEngine),
      platform({ guest }, { guest = it }),
    )

    viewModel.likeReview(reviewId = 7, likedByMe = false)
    awaitReal { viewModel.uiState.first { it.requestError != null } }

    assertEquals(2, writeCount)
    assertEquals(1, issueCount)
    assertEquals("new", guest?.token)
    assertEquals("요청을 처리하지 못했어요. 다시 시도해 주세요.", viewModel.uiState.value.requestError)
  }

  @Test
  fun `게스트 좋아요 취소 401은 재발급하지 않는다`() = runViewModelTest {
    var issueCount = 0
    var writeCount = 0
    val writeEngine = MockEngine {
      writeCount += 1
      respond("", HttpStatusCode.Unauthorized)
    }
    val authEngine = MockEngine {
      issueCount += 1
      error("게스트 토큰을 재발급하면 안 됨")
    }
    val viewModel = viewModel(
      testClient(writeEngine),
      testClient(authEngine),
      platform(readGuest = { guest("old") }),
    )

    viewModel.likeReview(reviewId = 7, likedByMe = true)
    awaitReal { viewModel.uiState.first { it.requestError != null } }

    assertEquals(1, writeCount)
    assertEquals(0, issueCount)
    assertEquals("이 기기에서는 더 이상 수정할 수 없습니다.", viewModel.uiState.value.requestError)
  }

  @Test
  fun `게스트 감상 삭제 401은 재발급하지 않는다`() = runViewModelTest {
    var issueCount = 0
    var writeCount = 0
    val writeEngine = MockEngine {
      writeCount += 1
      respond("", HttpStatusCode.Unauthorized)
    }
    val authEngine = MockEngine {
      issueCount += 1
      error("게스트 토큰을 재발급하면 안 됨")
    }
    val viewModel = viewModel(
      testClient(writeEngine),
      testClient(authEngine),
      platform(readGuest = { guest("old") }),
    )

    viewModel.deleteReview(reviewId = 7)
    awaitReal { viewModel.uiState.first { it.requestError != null } }

    assertEquals(1, writeCount)
    assertEquals(0, issueCount)
    assertEquals("이 기기에서는 더 이상 수정할 수 없습니다.", viewModel.uiState.value.requestError)
  }

  @Test
  fun `첫 감상 작성 시도에서 게스트 닉네임을 발급한 뒤 시트를 준비한다`() = runViewModelTest {
    var guest: GuestAuth? = null
    val ready = CompletableDeferred<Unit>()
    val client = testClient(MockEngine {
      respond(
        content = """{"guestToken":"new","nickname":"다정한 파란 참새","expiresAt":"2026-09-25T09:00:00"}""",
        status = HttpStatusCode.Created,
        headers = jsonHeaders(),
      )
    })
    val viewModel = viewModel(client, client, platform({ guest }, { guest = it }))

    viewModel.openReviewComposer { ready.complete(Unit) }
    awaitReal { ready.await() }

    assertTrue(ready.isCompleted)
    assertEquals("다정한 파란 참새", guest?.nickname)
    assertEquals("다정한 파란 참새", viewModel.uiState.value.guestNickname)
  }

  @Test
  fun `서재 미등록 책은 등록 응답 ID로 별점을 저장하고 즉시 반영한다`() = runViewModelTest {
    val requestedPaths = mutableListOf<String>()
    var detailGetCount = 0
    val engine = MockEngine { request ->
      requestedPaths += request.url.encodedPath
      when {
        request.method == HttpMethod.Get && request.url.encodedPath.contains("/by-isbn/") -> {
          detailGetCount += 1
          respond(if (detailGetCount == 1) DETAIL_WITHOUT_RECORD else DETAIL_AFTER_RATING, headers = jsonHeaders())
        }
        request.method == HttpMethod.Get -> respond(EMPTY_REVIEWS, headers = jsonHeaders())
        request.method == HttpMethod.Post -> respond(LIBRARY_RECORD, HttpStatusCode.Created, jsonHeaders())
        request.method == HttpMethod.Put -> respond(RATED_LIBRARY_RECORD, headers = jsonHeaders())
        else -> error("Unexpected request: ${request.method} ${request.url.encodedPath}")
      }
    }
    val client = testClient(engine)
    val viewModel = viewModel(client, client, platform(readGuest = { null }))
    viewModel.open(book().copy(isbn13 = "9780000000042"), accessToken = "access-token")
    awaitReal { viewModel.uiState.first { it.detail != null } }

    viewModel.saveRating(Rating.ofScore(5f))
    awaitReal { viewModel.uiState.first { it.detail?.averageRating == 5.0 } }

    assertTrue("/api/v1/library/73/rating" in requestedPaths)
    assertEquals(73L, viewModel.uiState.value.detail?.bookId)
    assertEquals(5.0, viewModel.uiState.value.detail?.myRecord?.rating)
  }

  @Test
  fun `상세 로딩 중 별점 저장은 기존 서재 ID를 기다리고 평균을 재조회한다`() = runViewModelTest {
    val firstDetailStarted = CompletableDeferred<Unit>()
    val finishFirstDetail = CompletableDeferred<Unit>()
    val requestedPaths = mutableListOf<String>()
    var detailGetCount = 0
    var successCount = 0
    val engine = MockEngine { request ->
      requestedPaths += request.url.encodedPath
      when {
        request.method == HttpMethod.Get && request.url.encodedPath.contains("/by-isbn/") -> {
          detailGetCount += 1
          if (detailGetCount == 1) {
            firstDetailStarted.complete(Unit)
            finishFirstDetail.await()
            respond(DETAIL_WITH_RECORD_AND_AVERAGE, headers = jsonHeaders())
          } else {
            respond(DETAIL_AFTER_EXISTING_RATING, headers = jsonHeaders())
          }
        }
        request.method == HttpMethod.Get -> respond(EMPTY_REVIEWS, headers = jsonHeaders())
        request.method == HttpMethod.Put -> respond(
          """{"bookId":42,"status":"READING","currentPage":12,"rating":5.0}""",
          headers = jsonHeaders(),
        )
        request.method == HttpMethod.Post -> error("기존 서재 책을 다시 등록하면 안 됨")
        else -> error("Unexpected request: ${request.method} ${request.url.encodedPath}")
      }
    }
    val client = testClient(engine)
    val viewModel = viewModel(client, client, platform(readGuest = { null }))

    viewModel.open(book().copy(isbn13 = "9780000000042"), accessToken = "access-token")
    runCurrent()
    awaitReal { firstDetailStarted.await() }
    viewModel.saveRating(Rating.ofScore(5f)) { successCount += 1 }
    runCurrent()

    assertEquals(0, requestedPaths.count { it.endsWith("/rating") })
    finishFirstDetail.complete(Unit)
    awaitReal { viewModel.uiState.first { it.detail?.averageRating == 4.2 } }

    assertEquals(2, detailGetCount)
    assertTrue("/api/v1/library/42/rating" in requestedPaths)
    assertEquals(0, requestedPaths.count { it == "/api/v1/library" })
    assertEquals(1, successCount)
    assertEquals(5.0, viewModel.uiState.value.detail?.myRecord?.rating)
    assertEquals(4.2, viewModel.uiState.value.detail?.averageRating)
    assertEquals(11, viewModel.uiState.value.detail?.ratingCount)
  }

  @Test
  fun `자동 서재 등록 뒤 별점 실패도 등록 성공을 즉시 알린다`() = runViewModelTest {
    var libraryAddedCount = 0
    var successCount = 0
    val requestedPaths = mutableListOf<String>()
    val engine = MockEngine { request ->
      requestedPaths += request.url.encodedPath
      when {
        request.method == HttpMethod.Get && request.url.encodedPath.contains("/by-isbn/") ->
          respond(DETAIL_WITHOUT_RECORD, headers = jsonHeaders())
        request.method == HttpMethod.Get -> respond(EMPTY_REVIEWS, headers = jsonHeaders())
        request.method == HttpMethod.Post -> respond(LIBRARY_RECORD, HttpStatusCode.Created, jsonHeaders())
        request.method == HttpMethod.Put -> respond("", HttpStatusCode.InternalServerError)
        else -> error("Unexpected request: ${request.method} ${request.url.encodedPath}")
      }
    }
    val client = testClient(engine)
    val viewModel = viewModel(client, client, platform(readGuest = { null }))
    viewModel.open(book().copy(isbn13 = "9780000000042"), accessToken = "access-token")
    advanceUntilIdle()

    viewModel.saveRating(
      Rating.ofScore(5f),
      onSuccess = { successCount += 1 },
      onLibraryAdded = { libraryAddedCount += 1 },
    )
    awaitReal { viewModel.uiState.first { it.requestError != null } }

    assertTrue("/api/v1/library/73/rating" in requestedPaths)
    assertEquals(1, libraryAddedCount)
    assertEquals(0, successCount)
    assertEquals(73L, viewModel.uiState.value.detail?.myRecord?.bookId)
    assertNull(viewModel.uiState.value.detail?.myRecord?.rating)
    assertEquals("요청을 처리하지 못했어요. 다시 시도해 주세요.", viewModel.uiState.value.requestError)
  }

  @Test
  fun `상태와 쪽수 변경 응답을 재조회 없이 즉시 반영한다`() = runViewModelTest {
    var currentPage = 12
    var status = "READING"
    val engine = MockEngine { request ->
      when {
        request.method == HttpMethod.Get && request.url.encodedPath.contains("/by-isbn/") ->
          respond(DETAIL_WITH_RECORD, headers = jsonHeaders())
        request.method == HttpMethod.Get -> respond(EMPTY_REVIEWS, headers = jsonHeaders())
        request.method == HttpMethod.Patch -> {
          if (request.url.encodedPath.endsWith("/42")) {
            if (currentPage == 12) currentPage = 80 else status = "FINISHED"
          }
          respond(
            """{"bookId":42,"status":"$status","currentPage":$currentPage,"rating":4.0}""",
            headers = jsonHeaders(),
          )
        }
        else -> error("Unexpected request: ${request.method} ${request.url.encodedPath}")
      }
    }
    val client = testClient(engine)
    val viewModel = viewModel(client, client, platform(readGuest = { null }))
    viewModel.open(book().copy(isbn13 = "9780000000042"), accessToken = "access-token")
    awaitReal { viewModel.uiState.first { it.detail != null } }

    viewModel.savePage(80)
    awaitReal { viewModel.uiState.first { it.detail?.myRecord?.currentPage == 80 } }
    assertEquals(80, viewModel.uiState.value.detail?.myRecord?.currentPage)

    viewModel.updateStatus(ReadingStatus.Finished)
    awaitReal { viewModel.uiState.first { it.detail?.myRecord?.status == "FINISHED" } }
    assertEquals("FINISHED", viewModel.uiState.value.detail?.myRecord?.status)
  }

  @Test
  fun `로그인 토큰이 바뀌면 상세 요청도 새 토큰을 사용한다`() = runViewModelTest {
    val authorizations = mutableListOf<String?>()
    val oldDetailStarted = CompletableDeferred<Unit>()
    val finishOldDetail = CompletableDeferred<Unit>()
    val engine = MockEngine { request ->
      if (!request.url.encodedPath.contains("/by-isbn/")) return@MockEngine respond(
        EMPTY_REVIEWS,
        headers = jsonHeaders(),
      )
      val authorization = request.headers[HttpHeaders.Authorization]
      authorizations += authorization
      if (authorization == "Bearer old-token") {
        oldDetailStarted.complete(Unit)
        finishOldDetail.await()
        respond(DETAIL_WITH_RECORD, headers = jsonHeaders())
      } else {
        respond(DETAIL_AFTER_AUTH_CHANGE, headers = jsonHeaders())
      }
    }
    val client = testClient(engine)
    val viewModel = viewModel(client, client, platform(readGuest = { null }))
    viewModel.open(book().copy(isbn13 = "9780000000042"), accessToken = "old-token")
    runCurrent()
    awaitReal { oldDetailStarted.await() }

    viewModel.syncAuthentication("new-token")
    finishOldDetail.complete(Unit)
    awaitReal { viewModel.uiState.first { it.detail?.myRecord?.currentPage == 7 } }

    assertEquals(listOf<String?>("Bearer old-token", "Bearer new-token"), authorizations)
    assertEquals(7, viewModel.uiState.value.detail?.myRecord?.currentPage)
  }

  @Test
  fun `계정 전환 전 mutation 결과는 새 계정 상태에 반영하지 않는다`() = runViewModelTest {
    val oldMutationStarted = CompletableDeferred<Unit>()
    val finishOldMutation = CompletableDeferred<Unit>()
    var successCount = 0
    val engine = MockEngine { request ->
      when {
        request.method == HttpMethod.Get && request.url.encodedPath.contains("/by-isbn/") -> {
          val detail = if (request.headers[HttpHeaders.Authorization] == "Bearer new-token") {
            DETAIL_AFTER_AUTH_CHANGE
          } else {
            DETAIL_WITH_RECORD
          }
          respond(detail, headers = jsonHeaders())
        }
        request.method == HttpMethod.Get -> respond(EMPTY_REVIEWS, headers = jsonHeaders())
        request.method == HttpMethod.Patch -> {
          oldMutationStarted.complete(Unit)
          finishOldMutation.await()
          respond(
            """{"bookId":42,"status":"READING","currentPage":80,"rating":4.0}""",
            headers = jsonHeaders(),
          )
        }
        else -> error("Unexpected request: ${request.method} ${request.url.encodedPath}")
      }
    }
    val client = testClient(engine)
    val viewModel = viewModel(client, client, platform(readGuest = { null }))
    viewModel.open(book().copy(isbn13 = "9780000000042"), accessToken = "old-token")
    awaitReal { viewModel.uiState.first { it.detail != null } }

    viewModel.savePage(80) { successCount += 1 }
    awaitReal { oldMutationStarted.await() }
    viewModel.syncAuthentication("new-token")
    finishOldMutation.complete(Unit)
    awaitReal { viewModel.uiState.first { it.detail?.myRecord?.currentPage == 7 } }

    assertEquals(0, successCount)
    assertEquals(7, viewModel.uiState.value.detail?.myRecord?.currentPage)
  }

  @Test
  fun `선택 별점 기준으로 낮은 같은 높은 평점 기록을 조회한다`() = runViewModelTest {
    var comparisonQuery: String? = null
    var comparisonIsbn: String? = null
    var comparisonAuthorization: String? = null
    val engine = MockEngine { request ->
      when {
        request.url.encodedPath == "/api/v1/members/me/ratings/comparison" -> {
          comparisonQuery = request.url.parameters["criterion"]
          comparisonIsbn = request.url.parameters["isbn13"]
          comparisonAuthorization = request.headers[HttpHeaders.Authorization]
          respond(
            """{"lower":{"bookId":1,"title":"낮은 책","myRating":4.0,"ratingUpdatedAt":"2026-08-01T00:00:00Z"},"current":{"bookId":2,"title":"같은 책","myRating":4.5,"ratingUpdatedAt":"2026-08-02T00:00:00Z"},"higher":{"bookId":3,"title":"높은 책","myRating":4.8,"ratingUpdatedAt":"2026-08-03T00:00:00Z"}}""",
            headers = jsonHeaders(),
          )
        }
        request.url.encodedPath.contains("/by-isbn/") -> respond(DETAIL_WITHOUT_RECORD, headers = jsonHeaders())
        else -> respond(EMPTY_REVIEWS, headers = jsonHeaders())
      }
    }
    val client = testClient(engine)
    val viewModel = viewModel(client, client, platform(readGuest = { null }))
    viewModel.open(book().copy(isbn13 = "9780000000042"), accessToken = "access-token")
    awaitReal { viewModel.uiState.first { it.detail != null } }

    viewModel.loadRatingComparison(Rating.ofScore(4.5f))
    awaitReal { viewModel.uiState.first { it.ratingComparison.size == 3 } }

    assertEquals("4.5", comparisonQuery)
    assertEquals("9780000000042", comparisonIsbn)
    assertEquals("Bearer access-token", comparisonAuthorization)
    assertEquals(listOf("낮은 책", "같은 책", "높은 책"), viewModel.uiState.value.ratingComparison.map { it.title })
    assertEquals(4.8, viewModel.uiState.value.ratingComparison.last().rating)
    assertEquals("2026.08.03", viewModel.uiState.value.ratingComparison.last().ratedAtLabel)
  }

  @Test
  fun `감상 삭제 성공은 재조회 없이 목록에서 제거한다`() = runViewModelTest {
    var reviewGetCount = 0
    val engine = MockEngine { request ->
      when {
        request.method == HttpMethod.Delete -> respond("", HttpStatusCode.NoContent)
        request.url.encodedPath.contains("/by-isbn/") -> respond(
          """{"bookId":42,"isbn13":"9780000000042","title":"책","authors":[],"translators":[],"publisher":"출판사","category":"소설","coverImageUrl":""}""",
          headers = jsonHeaders(),
        )
        else -> {
          reviewGetCount += 1
          respond(
            """{"totalCount":1,"nextPage":null,"items":[$REVIEW_RESPONSE]}""",
            headers = jsonHeaders(),
          )
        }
      }
    }
    val client = testClient(engine)
    val viewModel = viewModel(client, client, platform(readGuest = { guest("old") }))
    viewModel.open(book().copy(isbn13 = "9780000000042"), accessToken = null)
    awaitReal { viewModel.uiState.first { it.reviews.isNotEmpty() } }

    viewModel.deleteReview(reviewId = 7)
    awaitReal { viewModel.uiState.first { it.reviews.isEmpty() && it.reviewCount == 0 } }

    assertEquals(1, reviewGetCount)
  }

  private fun viewModel(
    writeClient: HttpClient,
    authClient: HttpClient,
    callbacks: AuthPlatformCallbacks,
  ) = BookDetailViewModel(
    repository = BookDetailRemoteRepository(writeClient),
    libraryRepository = LibraryRemoteRepository(writeClient),
    authPlatform = callbacks,
    authRepository = MobileAuthRemoteRepository(authClient),
  )

  private fun platform(
    readGuest: () -> GuestAuth?,
    writeGuest: (GuestAuth) -> Unit = {},
  ) = AuthPlatformCallbacks(
    requestGoogleIdToken = {},
    readRefreshToken = { null },
    writeRefreshToken = {},
    clearRefreshToken = {},
    readGuest = readGuest,
    writeGuest = writeGuest,
  )

  private fun book() = BookDetailArgs(id = "42", bookId = 42, title = "책")

  private fun guest(token: String) = GuestAuth(token, "게스트", "2026-09-25T09:00:00")

  private fun testClient(engine: MockEngine) = HttpClient(engine) {
    expectSuccess = true
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
  }

  private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, "application/json")

  private suspend fun <T> awaitReal(block: suspend () -> T): T =
    withContext(Dispatchers.Default) { withTimeout(5_000) { block() } }

  private fun runViewModelTest(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
      block()
    } finally {
      Dispatchers.resetMain()
    }
  }

  private companion object {
    const val EMPTY_REVIEWS = """{"totalCount":0,"nextPage":null,"items":[]}"""
    const val DETAIL_WITHOUT_RECORD =
      """{"bookId":42,"isbn13":"9780000000042","title":"책","authors":[],"translators":[],"publisher":"출판사","category":"소설","coverImageUrl":"","myRecord":null}"""
    const val DETAIL_WITH_RECORD =
      """{"bookId":42,"isbn13":"9780000000042","title":"책","authors":[],"translators":[],"publisher":"출판사","category":"소설","coverImageUrl":"","myRecord":{"status":"READING","currentPage":12,"myRating":4.0}}"""
    const val DETAIL_WITH_RECORD_AND_AVERAGE =
      """{"bookId":42,"isbn13":"9780000000042","title":"책","authors":[],"translators":[],"publisher":"출판사","category":"소설","coverImageUrl":"","averageRating":3.8,"ratingCount":10,"myRecord":{"status":"READING","currentPage":12,"myRating":4.0}}"""
    const val DETAIL_AFTER_EXISTING_RATING =
      """{"bookId":42,"isbn13":"9780000000042","title":"책","authors":[],"translators":[],"publisher":"출판사","category":"소설","coverImageUrl":"","averageRating":4.2,"ratingCount":11,"myRecord":{"status":"READING","currentPage":12,"myRating":5.0}}"""
    const val DETAIL_AFTER_RATING =
      """{"bookId":73,"isbn13":"9780000000042","title":"책","authors":[],"translators":[],"publisher":"출판사","category":"소설","coverImageUrl":"","averageRating":5.0,"ratingCount":1,"myRecord":{"status":"READING","currentPage":0,"myRating":5.0}}"""
    const val DETAIL_AFTER_AUTH_CHANGE =
      """{"bookId":42,"isbn13":"9780000000042","title":"책","authors":[],"translators":[],"publisher":"출판사","category":"소설","coverImageUrl":"","myRecord":{"status":"READING","currentPage":7,"myRating":3.0}}"""
    const val LIBRARY_RECORD = """{"bookId":73,"status":"READING","currentPage":0,"rating":null}"""
    const val RATED_LIBRARY_RECORD = """{"bookId":73,"status":"READING","currentPage":0,"rating":5.0}"""
    const val REVIEW_RESPONSE =
      """{"reviewId":7,"content":"감상","createdAt":"2026-08-27T00:00:00Z","author":{"displayName":"게스트","anonymous":false,"mine":true,"actorType":"GUEST"},"replyCount":0,"likeCount":0,"likedByMe":false,"isSpoiler":false,"recentReplies":[],"deleted":false}"""
  }
}
