package com.chaekchaek.app.ui.bookdetail

import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.auth.GuestAuth
import com.chaekchaek.app.data.remote.BookDetailRemoteRepository
import com.chaekchaek.app.data.remote.LibraryRemoteRepository
import com.chaekchaek.app.data.remote.MobileAuthRemoteRepository
import com.chaekchaek.app.data.remote.ReviewCreateRequest
import com.chaekchaek.app.data.remote.WriteCredential
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
    const val REVIEW_RESPONSE =
      """{"reviewId":7,"content":"감상","createdAt":"2026-08-27T00:00:00Z","author":{"displayName":"게스트","anonymous":false,"mine":true,"actorType":"GUEST"},"replyCount":0,"likeCount":0,"likedByMe":false,"isSpoiler":false,"recentReplies":[],"deleted":false}"""
  }
}
