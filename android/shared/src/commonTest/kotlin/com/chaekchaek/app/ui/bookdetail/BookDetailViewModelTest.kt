package com.chaekchaek.app.ui.bookdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.chaekchaek.app.analytics.AnalyticsEvent
import com.chaekchaek.app.analytics.AnalyticsTracker
import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.auth.GuestAuth
import com.chaekchaek.app.data.remote.BookDetailRemoteRepository
import com.chaekchaek.app.data.remote.LibraryRecord
import com.chaekchaek.app.data.remote.LibraryRemoteRepository
import com.chaekchaek.app.data.remote.MobileAuthRemoteRepository
import com.chaekchaek.app.data.remote.ReviewCreateRequest
import com.chaekchaek.app.data.remote.ReviewScope
import com.chaekchaek.app.data.remote.ReviewSort
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
  fun `바로 담기 완료는 열린 상세의 기록 수정에서 중복 등록하지 않는다`() = runViewModelTest {
    val writes = mutableListOf<String>()
    val client = testClient(MockEngine { request ->
      when {
        request.url.encodedPath.contains("/by-isbn/") -> respond(DETAIL_WITHOUT_RECORD, headers = jsonHeaders())
        request.method == HttpMethod.Patch -> {
          writes += request.url.encodedPath
          respond(LIBRARY_RECORD.replace("\"currentPage\":0", "\"currentPage\":18"), headers = jsonHeaders())
        }
        request.method == HttpMethod.Post -> error("저장된 책을 다시 등록하면 안 된다")
        else -> respond(EMPTY_REVIEWS, headers = jsonHeaders())
      }
    })
    val vm = viewModel(client, client, platform({ null }))
    val target = book().copy(isbn13 = "9780000000042")
    vm.open(target, "member")
    awaitReal { vm.uiState.first { it.detail != null } }
    vm.acceptSavedLibraryRecord(target.isbn13, LibraryRecord("READING", 0, null, 73))
    vm.savePage(18)
    awaitReal { vm.uiState.first { it.detail?.myRecord?.currentPage == 18 } }
    assertEquals(listOf("/api/v1/library/73"), writes)
  }

  @Test
  fun `바로 담기보다 먼저 시작한 상세 응답은 저장 기록을 지우지 않는다`() = runViewModelTest {
    val detailRequested = CompletableDeferred<Unit>()
    val detailResponse = CompletableDeferred<Unit>()
    val client = testClient(MockEngine { request ->
      if (request.url.encodedPath.contains("/by-isbn/")) {
        detailRequested.complete(Unit)
        detailResponse.await()
        respond(DETAIL_WITHOUT_RECORD, headers = jsonHeaders())
      } else respond(EMPTY_REVIEWS, headers = jsonHeaders())
    })
    val vm = viewModel(client, client, platform({ null }))
    val target = book().copy(isbn13 = "9780000000042")
    vm.open(target, "member")
    awaitReal { detailRequested.await() }
    val record = LibraryRecord("READING", 0, null, 73)
    vm.acceptSavedLibraryRecord(target.isbn13, record)
    detailResponse.complete(Unit)
    awaitReal { vm.uiState.first { it.detail != null } }
    assertEquals(record, vm.uiState.value.detail?.myRecord)
    vm.acceptSavedLibraryRecord("9780000000099", LibraryRecord("COMPLETED", 99, null, 99))
    assertEquals(record, vm.uiState.value.detail?.myRecord)
  }

  @Test
  fun `담기를 반복해도 삭제하지 않고 명시적 삭제만 서재를 지운다`() = runViewModelTest {
    val paths = mutableListOf<String>()
    val engine = MockEngine { request ->
      paths += request.url.encodedPath
      when {
        request.url.encodedPath.contains("/by-isbn/") -> respond(DETAIL_WITHOUT_RECORD, headers = jsonHeaders())
        request.url.encodedPath == "/api/v1/library" -> respond(LIBRARY_RECORD, headers = jsonHeaders())
        request.url.encodedPath.endsWith("/bulk-delete") -> respond("", HttpStatusCode.NoContent)
        else -> respond(EMPTY_REVIEWS, headers = jsonHeaders())
      }
    }
    val client = testClient(engine)
    val vm = viewModel(client, client, platform({ null }))
    vm.open(book().copy(isbn13 = "9780000000042"), "member")
    awaitReal { vm.uiState.first { it.detail != null } }
    vm.addToLibrary()
    vm.addToLibrary()
    awaitReal { vm.uiState.first { it.detail?.myRecord != null && !it.isSavingLibrary } }
    vm.addToLibrary()
    runCurrent()
    assertEquals(1, paths.count { it == "/api/v1/library" })
    assertFalse(paths.any { it.endsWith("/bulk-delete") })
    assertEquals("내 서재에 담았어요", vm.uiState.value.libraryMessage)
    vm.removeFromLibrary(73)
    awaitReal { vm.uiState.first { it.libraryMessage == "서재에서 삭제했어요" } }
    assertNull(vm.uiState.value.detail?.myRecord)
    assertEquals(1, paths.count { it.endsWith("/bulk-delete") })
  }

  @Test
  fun `읽는 중이 아닌 책도 실패 뒤 같은 감상을 재시도하고 성공할 때만 작성을 완료한다`() = runViewModelTest {
    for (status in listOf(null, "WANT_TO_READ", "FINISHED")) {
      val firstStarted = CompletableDeferred<Unit>()
      val firstResponse = CompletableDeferred<Unit>()
      val retryStarted = CompletableDeferred<Unit>()
      val retryResponse = CompletableDeferred<Unit>()
      val submissions = mutableListOf<String>()
      val events = MutableStateFlow<List<AnalyticsEvent>>(emptyList())
      val detail = if (status == null) DETAIL_WITHOUT_RECORD else
        DETAIL_WITH_RECORD.replace("READING", status)
      val client = testClient(MockEngine { request ->
        when {
          request.method == HttpMethod.Post -> {
            submissions += (request.body as io.ktor.http.content.TextContent).text
            if (submissions.size == 1) {
              firstStarted.complete(Unit)
              firstResponse.await()
              respond("", HttpStatusCode.InternalServerError)
            } else {
              retryStarted.complete(Unit)
              retryResponse.await()
              respond("""{"bookId":42,"review":$REVIEW_RESPONSE}""", HttpStatusCode.Created, jsonHeaders())
            }
          }
          request.url.encodedPath.contains("/by-isbn/") -> respond(detail, headers = jsonHeaders())
          else -> respond(EMPTY_REVIEWS, headers = jsonHeaders())
        }
      })
      val viewModel = viewModel(client, client, platform({ null }),
        AnalyticsTracker("test") { event -> events.update { it + event } })
      val store = ViewModelStore().also { it.put("review", viewModel) }
      viewModel.open(book().copy(isbn13 = "9780000000042"), "fixture-member")
      awaitReal { viewModel.uiState.first { it.detail != null } }
      viewModel.openReviewComposer {}
      val request = ReviewCreateRequest("보존할 감상", "문장", "첫 장", 80, 224, true)
      viewModel.createReview(request)
      assertEquals(ReviewSubmissionState.Submitting, viewModel.uiState.value.reviewSubmissionState)
      viewModel.createReview(request)
      awaitReal { firstStarted.await() }
      assertEquals(1, submissions.size)
      firstResponse.complete(Unit)
      awaitReal { viewModel.uiState.first { it.reviewSubmissionState == ReviewSubmissionState.Failed } }
      assertTrue(viewModel.uiState.value.requestError != null)
      assertFalse(viewModel.uiState.value.isSubmitting)

      viewModel.createReview(request)
      awaitReal { retryStarted.await() }
      assertEquals(ReviewSubmissionState.Submitting, viewModel.uiState.value.reviewSubmissionState)
      assertNull(viewModel.uiState.value.requestError)
      retryResponse.complete(Unit)
      awaitReal { viewModel.uiState.first { it.reviewSubmissionState == ReviewSubmissionState.Succeeded } }
      assertEquals(2, submissions.size)
      assertEquals(submissions.first(), submissions.last())
      val actions = events.value.filter { it.name == "cc_action_start" && it.stringParameters["action"] == "review_create" }
      assertEquals(2, actions.size)
      assertTrue(actions.first().stringParameters["composer_id"] != null)
      assertEquals(actions.first().stringParameters["composer_id"], actions.last().stringParameters["composer_id"])
      viewModel.openReviewComposer {}
      assertEquals(ReviewSubmissionState.Idle, viewModel.uiState.value.reviewSubmissionState)
      store.clear()
      advanceUntilIdle()
      client.close()
    }
  }

  @Test
  fun `회원 복원이 실패해 내 감상이 해제되면 전체 감상 첫 페이지만 읽는다`() = runViewModelTest {
    val requestedPages = mutableListOf<Int>()
    val client = testClient(MockEngine { request ->
      if (request.url.encodedPath.contains("/by-isbn/")) {
        respond(DETAIL_WITHOUT_RECORD, headers = jsonHeaders())
      } else {
        val page = request.url.parameters["page"]!!.toInt()
        requestedPages += page
        respond("""{"totalCount":2,"nextPage":${if (page == 1) "2" else "null"},"items":[$REVIEW_RESPONSE]}""",
          headers = jsonHeaders())
      }
    })
    val savedState = SavedStateHandle(mapOf(
      BookDetailSavedStateKeys.Opened to true,
      BookDetailSavedStateKeys.LoadedPage to 2,
      BookDetailSavedStateKeys.ReviewScope to ReviewScope.MINE.name,
    ))
    val viewModel = viewModel(client, client, platform({ null }), savedStateHandle = savedState)
    viewModel.open(book().copy(isbn13 = "9780000000042"), null)
    awaitReal { viewModel.uiState.first { it.detail != null } }
    assertEquals(ReviewScope.ALL, viewModel.uiState.value.reviewScope)
    assertEquals(1, savedState.get<Int>(BookDetailSavedStateKeys.LoadedPage))
    assertEquals(listOf(1), requestedPages)
  }

  @Test
  fun `같은 회원의 토큰 갱신과 상세 재연결은 내 감상 조건과 페이지를 유지한다`() = runViewModelTest {
    val requestedPages = mutableListOf<Int>()
    val client = testClient(MockEngine { request ->
      if (request.url.encodedPath.contains("/by-isbn/")) {
        respond(DETAIL_WITHOUT_RECORD, headers = jsonHeaders())
      } else {
        val page = request.url.parameters["page"]!!.toInt()
        requestedPages += page
        val review = REVIEW_RESPONSE.replace("\"reviewId\":7", "\"reviewId\":$page")
        respond("""{"totalCount":2,"nextPage":${if (page == 1) "2" else "null"},"items":[$review]}""",
          headers = jsonHeaders())
      }
    })
    val savedState = SavedStateHandle(mapOf(
      BookDetailSavedStateKeys.Opened to true,
      BookDetailSavedStateKeys.LoadedPage to 2,
      BookDetailSavedStateKeys.ReviewScope to ReviewScope.MINE.name,
      BookDetailSavedStateKeys.ReviewSort to ReviewSort.PAGE.name,
    ))
    val viewModel = viewModel(client, client, platform({ null }), savedStateHandle = savedState)
    val target = book().copy(isbn13 = "9780000000042")
    viewModel.open(target, "member-before-renewal")
    awaitReal { viewModel.uiState.first { it.detail != null } }

    viewModel.syncAuthentication("member-after-renewal")
    assertEquals(ReviewScope.MINE, viewModel.uiState.value.reviewScope)
    assertEquals(2, savedState.get<Int>(BookDetailSavedStateKeys.LoadedPage))
    awaitReal { while (requestedPages.size < 4) kotlinx.coroutines.yield() }
    viewModel.open(target, "member-after-recreation")
    assertEquals(ReviewScope.MINE, viewModel.uiState.value.reviewScope)
    assertEquals(ReviewSort.PAGE, viewModel.uiState.value.reviewSort)
    assertEquals(2, savedState.get<Int>(BookDetailSavedStateKeys.LoadedPage))

    viewModel.signOut()
    assertEquals(ReviewScope.ALL, viewModel.uiState.value.reviewScope)
    assertEquals(1, savedState.get<Int>(BookDetailSavedStateKeys.LoadedPage))
  }

  @Test
  fun `복원 조회 실패는 대기를 해제하고 기존 다시 시도로 복구한다`() = runViewModelTest {
    var fails = true
    val client = testClient(MockEngine { request ->
      when {
        fails -> respond("", HttpStatusCode.InternalServerError)
        request.url.encodedPath.contains("/by-isbn/") -> respond(DETAIL_WITHOUT_RECORD, headers = jsonHeaders())
        else -> respond(EMPTY_REVIEWS, headers = jsonHeaders())
      }
    })
    val state = SavedStateHandle(mapOf(BookDetailSavedStateKeys.Opened to true))
    val viewModel = viewModel(client, client, platform({ null }), savedStateHandle = state)
    viewModel.open(book().copy(isbn13 = "9780000000042"), null)
    awaitReal { viewModel.uiState.first { it.reviewPageLoadState == ReviewPageLoadState.RefreshFailed } }
    assertFalse(viewModel.uiState.value.isRestoring)

    fails = false
    viewModel.retryReviews()
    awaitReal { viewModel.uiState.first { it.detail != null } }
    assertEquals(ReviewPageLoadState.Idle, viewModel.uiState.value.reviewPageLoadState)
  }

  @Test
  fun `상세 복원은 읽던 감상 페이지까지 재조회하고 복원 대기를 해제한다`() = runViewModelTest {
    val requestedPages = mutableListOf<Int>()
    val client = testClient(MockEngine { request ->
      if (request.url.encodedPath.contains("/by-isbn/")) {
        respond(DETAIL_WITHOUT_RECORD, headers = jsonHeaders())
      } else {
        val page = request.url.parameters["page"]!!.toInt()
        requestedPages += page
        val review = REVIEW_RESPONSE.replace("\"reviewId\":7", "\"reviewId\":$page")
        respond("""{"totalCount":2,"nextPage":${if (page == 1) "2" else "null"},"items":[$review]}""",
          headers = jsonHeaders())
      }
    })
    val savedState = SavedStateHandle(mapOf(
      BookDetailSavedStateKeys.Opened to true,
      BookDetailSavedStateKeys.LoadedPage to 2,
    ))
    val viewModel = viewModel(client, client, platform({ null }), savedStateHandle = savedState)
    viewModel.open(book().copy(isbn13 = "9780000000042"), null)
    assertTrue(viewModel.uiState.value.isRestoring)

    awaitReal { viewModel.uiState.first { it.detail != null } }

    assertEquals(listOf(1, 2), requestedPages)
    assertEquals(listOf(1L, 2L), viewModel.uiState.value.reviews.map { it.reviewId })
    assertFalse(viewModel.uiState.value.isRestoring)
  }

  @Test
  fun `상세 복원은 필터와 정렬만 유지하고 대기 중 액션은 재생하지 않는다`() = runViewModelTest {
    val client = testClient(MockEngine { error("네트워크를 호출하면 안 됨") })
    val savedStateHandle = SavedStateHandle(
      mapOf(
        BookDetailSavedStateKeys.ReviewScope to ReviewScope.MINE.name,
        BookDetailSavedStateKeys.ReviewSort to ReviewSort.PAGE.name,
      ),
    )
    val first = viewModel(client, client, platform(readGuest = { null }), savedStateHandle = savedStateHandle)

    assertEquals(ReviewScope.MINE, first.uiState.value.reviewScope)
    assertEquals(ReviewSort.PAGE, first.uiState.value.reviewSort)
    assertFalse(first.requestAuthentication(BookDetailAuthenticatedAction.AddToLibrary))

    val restored = viewModel(client, client, platform(readGuest = { null }), savedStateHandle = savedStateHandle)

    assertEquals(ReviewScope.MINE, restored.uiState.value.reviewScope)
    assertEquals(ReviewSort.PAGE, restored.uiState.value.reviewSort)
    assertNull(restored.uiState.value.pendingAction)
  }

  @Test
  fun `빠른 추가 조회 실패는 자동 반복하지 않고 명시적 재시도로 복구한다`() =
    verifyFailedPageRetry(showDelayedLoader = false)

  @Test
  fun `로더 표시 후 추가 조회 실패도 명시적 재시도로 복구한다`() =
    verifyFailedPageRetry(showDelayedLoader = true)

  private fun verifyFailedPageRetry(showDelayedLoader: Boolean) = runViewModelTest {
    val deleted = REVIEW_RESPONSE.replace("\"deleted\":false", "\"deleted\":true")
    val active = REVIEW_RESPONSE.replace("\"reviewId\":7", "\"reviewId\":8")
    val firstStarted = CompletableDeferred<Unit>()
    val failFirst = CompletableDeferred<Unit>()
    val retryStarted = CompletableDeferred<Unit>()
    val completeRetry = CompletableDeferred<Unit>()
    var pageRequests = 0
    val engine = MockEngine { request ->
      when {
        request.url.encodedPath.contains("/by-isbn/") -> respond(DETAIL_WITHOUT_RECORD, headers = jsonHeaders())
        request.url.parameters["page"] == "2" -> {
          pageRequests += 1
          if (pageRequests == 1) {
            firstStarted.complete(Unit)
            failFirst.await()
            error("페이지 조회 실패")
          }
          retryStarted.complete(Unit)
          completeRetry.await()
          respond("""{"totalCount":2,"nextPage":null,"items":[$active]}""", headers = jsonHeaders())
        }
        else -> respond("""{"totalCount":2,"nextPage":2,"items":[$deleted]}""", headers = jsonHeaders())
      }
    }
    val client = testClient(engine)
    val viewModel = viewModel(client, client, platform(readGuest = { null }))
    viewModel.open(book().copy(isbn13 = "9780000000042"), accessToken = null)
    awaitReal { viewModel.uiState.first { it.nextReviewPage == 2 } }
    viewModel.loadMoreReviews()
    awaitReal { firstStarted.await() }
    if (showDelayedLoader) {
      advanceTimeBy(API_LOADING_DELAY_MILLIS)
      runCurrent()
      assertTrue(viewModel.uiState.value.isLoadingMore)
    }
    failFirst.complete(Unit)
    awaitReal { viewModel.uiState.first { it.reviewPageLoadState == ReviewPageLoadState.Failed } }
    assertFalse(viewModel.uiState.value.isLoadingMore)
    assertNull(viewModel.uiState.value.requestError)
    assertTrue(visibleReviews(viewModel.uiState.value.reviews).isEmpty())
    repeat(3) { viewModel.loadMoreReviews() }
    runCurrent()
    assertEquals(1, pageRequests)

    viewModel.retryReviews()
    awaitReal { retryStarted.await() }
    assertEquals(ReviewPageLoadState.Retrying, viewModel.uiState.value.reviewPageLoadState)
    viewModel.retryReviews()
    viewModel.loadMoreReviews()
    runCurrent()
    assertEquals(2, pageRequests)
    completeRetry.complete(Unit)
    awaitReal { viewModel.uiState.first { it.nextReviewPage == null } }
    assertEquals(ReviewPageLoadState.Idle, viewModel.uiState.value.reviewPageLoadState)
    assertEquals(listOf(8L), visibleReviews(viewModel.uiState.value.reviews).map { it.reviewId })
    assertEquals(2, viewModel.uiState.value.reviews.size)
  }

  @Test
  fun `정렬 변경은 이전 페이지 실패 상태와 다음 페이지를 초기화한다`() = runViewModelTest {
    val firstPage = """{"totalCount":2,"nextPage":2,"items":[$REVIEW_RESPONSE]}"""
    val newSortStarted = CompletableDeferred<Unit>()
    val finishNewSort = CompletableDeferred<Unit>()
    var pageRequests = 0
    val engine = MockEngine { request ->
      when {
        request.url.encodedPath.contains("/by-isbn/") -> respond(DETAIL_WITHOUT_RECORD, headers = jsonHeaders())
        request.url.parameters["sort"] == "OLDEST" -> {
          newSortStarted.complete(Unit)
          finishNewSort.await()
          respond(EMPTY_REVIEWS, headers = jsonHeaders())
        }
        request.url.parameters["page"] == "2" -> {
          pageRequests += 1
          error("페이지 조회 실패")
        }
        else -> respond(firstPage, headers = jsonHeaders())
      }
    }
    val client = testClient(engine)
    val viewModel = viewModel(client, client, platform(readGuest = { null }))
    viewModel.open(book().copy(isbn13 = "9780000000042"), accessToken = null)
    awaitReal { viewModel.uiState.first { it.nextReviewPage == 2 } }
    viewModel.loadMoreReviews()
    awaitReal { viewModel.uiState.first { it.reviewPageLoadState == ReviewPageLoadState.Failed } }
    viewModel.changeReviewSort(com.chaekchaek.app.data.remote.ReviewSort.OLDEST)
    awaitReal { newSortStarted.await() }
    assertEquals(ReviewPageLoadState.Loading, viewModel.uiState.value.reviewPageLoadState)
    assertNull(viewModel.uiState.value.nextReviewPage)
    viewModel.retryReviews()
    viewModel.loadMoreReviews()
    runCurrent()
    assertEquals(1, pageRequests)
    finishNewSort.complete(Unit)
    awaitReal { viewModel.uiState.first { it.reviews.isEmpty() } }
    assertEquals(ReviewPageLoadState.Idle, viewModel.uiState.value.reviewPageLoadState)
  }

  @Test
  fun `좋아요 후 첫 페이지 새로고침 실패는 재시도로 목록과 다음 페이지를 복구한다`() =
    verifyRefreshFailureRetry(changeSort = false)

  @Test
  fun `정렬 첫 페이지 실패는 새 정렬로 재시도하고 이전 목록과 섞지 않는다`() =
    verifyRefreshFailureRetry(changeSort = true)

  private fun verifyRefreshFailureRetry(changeSort: Boolean) = runViewModelTest {
    val active = REVIEW_RESPONSE.replace("\"reviewId\":7", "\"reviewId\":8")
    val next = REVIEW_RESPONSE.replace("\"reviewId\":7", "\"reviewId\":9")
    val retryStarted = CompletableDeferred<Unit>()
    val finishRetry = CompletableDeferred<Unit>()
    var firstPageRequests = 0
    var nextPageRequests = 0
    var likes = 0
    val requestedSorts = mutableListOf<String?>()
    val engine = MockEngine { request ->
      when {
        request.method == HttpMethod.Post -> {
          likes += 1
          respond("""{"likeCount":1,"likedByMe":true}""", headers = jsonHeaders())
        }
        request.url.encodedPath.contains("/by-isbn/") -> respond(DETAIL_WITHOUT_RECORD, headers = jsonHeaders())
        request.url.parameters["page"] == "2" -> {
          nextPageRequests += 1
          respond("""{"totalCount":2,"nextPage":null,"items":[$next]}""", headers = jsonHeaders())
        }
        else -> {
          firstPageRequests += 1
          requestedSorts += request.url.parameters["sort"]
          when (firstPageRequests) {
            1 -> respond("""{"totalCount":2,"nextPage":2,"items":[$REVIEW_RESPONSE]}""", headers = jsonHeaders())
            2 -> error("첫 페이지 새로고침 실패")
            else -> {
              retryStarted.complete(Unit)
              finishRetry.await()
              respond("""{"totalCount":2,"nextPage":2,"items":[$active]}""", headers = jsonHeaders())
            }
          }
        }
      }
    }
    val client = testClient(engine)
    val viewModel = viewModel(client, client, platform(readGuest = { guest("test") }))
    viewModel.open(book().copy(isbn13 = "9780000000042"), accessToken = null)
    awaitReal { viewModel.uiState.first { it.nextReviewPage == 2 } }
    if (changeSort) viewModel.changeReviewSort(com.chaekchaek.app.data.remote.ReviewSort.OLDEST)
    else viewModel.likeReview(reviewId = 7, likedByMe = false)
    awaitReal { viewModel.uiState.first { it.reviewPageLoadState == ReviewPageLoadState.RefreshFailed } }
    assertEquals(listOf(7L), viewModel.uiState.value.reviews.map { it.reviewId })
    assertNull(viewModel.uiState.value.nextReviewPage)
    repeat(3) { viewModel.loadMoreReviews() }
    runCurrent()
    assertEquals(0, nextPageRequests)
    assertEquals(2, firstPageRequests)

    viewModel.retryReviews()
    awaitReal { retryStarted.await() }
    assertEquals(ReviewPageLoadState.RefreshRetrying, viewModel.uiState.value.reviewPageLoadState)
    viewModel.retryReviews()
    runCurrent()
    assertEquals(3, firstPageRequests)
    advanceTimeBy(API_LOADING_DELAY_MILLIS)
    runCurrent()
    assertTrue(viewModel.uiState.value.isLoading)
    finishRetry.complete(Unit)
    awaitReal { viewModel.uiState.first { it.reviewPageLoadState == ReviewPageLoadState.Idle } }
    assertEquals(listOf(8L), viewModel.uiState.value.reviews.map { it.reviewId })
    assertEquals(2, viewModel.uiState.value.nextReviewPage)
    assertFalse(viewModel.uiState.value.isLoading)
    assertEquals(if (changeSort) "OLDEST" else "LATEST", requestedSorts.last())
    assertEquals(if (changeSort) 0 else 1, likes)

    viewModel.loadMoreReviews()
    awaitReal { viewModel.uiState.first { it.nextReviewPage == null } }
    assertEquals(listOf(8L, 9L), viewModel.uiState.value.reviews.map { it.reviewId })
    assertEquals(1, nextPageRequests)
  }

  @Test
  fun `삭제 감상만 있는 첫 페이지 뒤의 정상 감상을 계속 표시한다`() = runViewModelTest {
    val deletedReview = REVIEW_RESPONSE.replace("\"deleted\":false", "\"deleted\":true")
    val activeReview = REVIEW_RESPONSE.replace("\"reviewId\":7", "\"reviewId\":8")
    val engine = MockEngine { request ->
      val response = when {
        request.url.encodedPath.contains("/by-isbn/") -> DETAIL_WITHOUT_RECORD
        request.url.parameters["page"] == "2" ->
          """{"totalCount":2,"nextPage":null,"items":[$activeReview]}"""
        else -> """{"totalCount":2,"nextPage":2,"items":[$deletedReview]}"""
      }
      respond(response, headers = jsonHeaders())
    }
    val client = testClient(engine)
    val viewModel = viewModel(client, client, platform(readGuest = { null }))

    viewModel.open(book().copy(isbn13 = "9780000000042"), accessToken = null)
    awaitReal { viewModel.uiState.first { it.nextReviewPage == 2 } }
    assertTrue(visibleReviews(viewModel.uiState.value.reviews).isEmpty())

    viewModel.loadMoreReviews()
    awaitReal { viewModel.uiState.first { it.reviews.size == 2 } }

    assertEquals(listOf(8L), visibleReviews(viewModel.uiState.value.reviews).map { it.reviewId })
    assertEquals(2, viewModel.uiState.value.reviewCount)
    assertNull(viewModel.uiState.value.nextReviewPage)
  }

  @Test
  fun `삭제된 감상을 서버가 다시 반환해도 재조회 후 표시하지 않는다`() = runViewModelTest {
    var deleted = false
    val engine = MockEngine { request ->
      if (request.method == HttpMethod.Delete) {
        deleted = true
        respond("", HttpStatusCode.NoContent)
      } else {
        val response = if (request.url.encodedPath.contains("/by-isbn/")) DETAIL_WITHOUT_RECORD else {
          val review = REVIEW_RESPONSE.replace("\"deleted\":false", "\"deleted\":$deleted")
          """{"totalCount":1,"nextPage":null,"items":[$review]}"""
        }
        respond(response, headers = jsonHeaders())
      }
    }
    val client = testClient(engine)
    val viewModel = viewModel(client, client, platform(readGuest = { guest("test") }))

    viewModel.open(book().copy(isbn13 = "9780000000042"), accessToken = null)
    awaitReal { viewModel.uiState.first { it.reviews.isNotEmpty() } }
    assertEquals(1, visibleReviews(viewModel.uiState.value.reviews).size)

    viewModel.deleteReview(7)
    awaitReal { viewModel.uiState.first { it.reviews.isEmpty() } }
    viewModel.retry()
    awaitReal { viewModel.uiState.first { it.reviews.any { review -> review.deleted } } }

    assertTrue(visibleReviews(viewModel.uiState.value.reviews).isEmpty())
    assertTrue(viewModel.uiState.value.reviews.single().deleted)
  }

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
  fun `ISBN 감상 작성은 지연 중인 상세 조회와 nullable bookId를 기다리지 않는다`() = runViewModelTest {
    val firstDetailStarted = CompletableDeferred<Unit>()
    val keepFirstDetailPending = CompletableDeferred<Unit>()
    val reviewWriteStarted = CompletableDeferred<Unit>()
    val keepReviewWritePending = CompletableDeferred<Unit>()
    val events = MutableStateFlow<List<AnalyticsEvent>>(emptyList())
    var detailRequestCount = 0
    val engine = MockEngine { request ->
      when {
        request.method == HttpMethod.Get && request.url.encodedPath.endsWith("/by-isbn/9780000000042") -> {
          detailRequestCount += 1
          if (detailRequestCount == 1) {
            firstDetailStarted.complete(Unit)
            keepFirstDetailPending.await()
          }
          respond(
            """{"bookId":${if (detailRequestCount == 1) "null" else "42"},"isbn13":"9780000000042","title":"책","authors":[],"translators":[],"publisher":"출판사","category":"소설","coverImageUrl":""}""",
            headers = jsonHeaders(),
          )
        }
        request.method == HttpMethod.Post && request.url.encodedPath.endsWith("/by-isbn/9780000000042/reviews") -> {
          reviewWriteStarted.complete(Unit)
          keepReviewWritePending.await()
          respond(
            """{"bookId":42,"review":$REVIEW_RESPONSE}""",
            status = HttpStatusCode.Created,
            headers = jsonHeaders(),
          )
        }
        else -> respond(EMPTY_REVIEWS, headers = jsonHeaders())
      }
    }
    val client = testClient(engine)
    val viewModel = viewModel(
      client,
      client,
      platform(readGuest = { guest("old") }),
      AnalyticsTracker("test") { event -> events.update { it + event } },
    )
    viewModel.open(
      book().copy(id = "9780000000042", isbn13 = "9780000000042", bookId = null),
      accessToken = null,
    )
    awaitReal { firstDetailStarted.await() }

    viewModel.createReview(ReviewCreateRequest("감상"))

    awaitReal { reviewWriteStarted.await() }
    keepFirstDetailPending.complete(Unit)
    awaitReal { events.first { logged -> logged.any { it.name == "cc_load_result" } } }
    assertFalse(
      "action_id" in events.value.last { it.name == "cc_load_result" }.stringParameters,
    )
    keepReviewWritePending.complete(Unit)
    awaitReal { viewModel.uiState.first { it.detail?.bookId == 42L } }
    assertNull(viewModel.uiState.value.requestError)
  }

  @Test
  fun `ISBN이 없는 감상 작성은 숫자 도서 ID 경로를 유지한다`() = runViewModelTest {
    var reviewWritePath = ""
    val reviewWriteStarted = CompletableDeferred<Unit>()
    val finishReviewWrite = CompletableDeferred<Unit>()
    val engine = MockEngine { request ->
      reviewWritePath = request.url.encodedPath
      reviewWriteStarted.complete(Unit)
      finishReviewWrite.await()
      respond(REVIEW_RESPONSE, status = HttpStatusCode.Created, headers = jsonHeaders())
    }
    val client = testClient(engine)
    val viewModel = viewModel(client, client, platform(readGuest = { guest("old") }))
    viewModel.open(book().copy(isbn13 = ""), accessToken = null)
    advanceUntilIdle()

    viewModel.createReview(ReviewCreateRequest("감상"))
    awaitReal { reviewWriteStarted.await() }
    advanceTimeBy(API_LOADING_DELAY_MILLIS)
    runCurrent()
    assertTrue(viewModel.uiState.value.isSubmitting)
    finishReviewWrite.complete(Unit)
    awaitReal { viewModel.uiState.first { !it.isSubmitting } }
    advanceUntilIdle()

    assertEquals("/api/v1/books/42/reviews", reviewWritePath)
    assertNull(viewModel.uiState.value.requestError)
  }

  @Test
  fun `감상 작성 실패는 요청 경로와 실패 단계와 HTTP 상태를 기록한다`() = runViewModelTest {
    val events = MutableStateFlow<List<AnalyticsEvent>>(emptyList())
    val engine = MockEngine { request ->
      if (request.method == HttpMethod.Post) {
        respond("", HttpStatusCode.InternalServerError)
      } else {
        respond(
          """{"bookId":null,"isbn13":"9780000000042","title":"책","authors":[],"translators":[],"publisher":"출판사","category":"소설","coverImageUrl":""}""",
          headers = jsonHeaders(),
        )
      }
    }
    val client = testClient(engine)
    val viewModel = viewModel(
      client,
      client,
      platform(readGuest = { guest("old") }),
      AnalyticsTracker("test") { event -> events.update { it + event } },
    )
    viewModel.open(
      book().copy(id = "9780000000042", isbn13 = "9780000000042", bookId = null),
      accessToken = null,
    )
    awaitReal { viewModel.uiState.first { it.detail != null } }

    viewModel.createReview(ReviewCreateRequest("감상"))
    awaitReal { events.first { logged -> logged.any { it.name == "cc_action_result" } } }

    val result = events.value.last { it.name == "cc_action_result" }
    assertEquals("failure", result.stringParameters["outcome"])
    assertEquals("by_isbn", result.stringParameters["request_route"])
    assertEquals("missing", result.stringParameters["book_id_state"])
    assertEquals("request", result.stringParameters["failure_stage"])
    assertEquals(500L, result.longParameters["http_status"])
  }

  @Test
  fun `감상 작성 대상을 해석하지 못하면 서버 요청 전 실패로 기록한다`() = runViewModelTest {
    val events = MutableStateFlow<List<AnalyticsEvent>>(emptyList())
    var requestCount = 0
    val client = testClient(MockEngine {
      requestCount += 1
      respond("", HttpStatusCode.InternalServerError)
    })
    val viewModel = viewModel(
      client,
      client,
      platform(readGuest = { guest("old") }),
      AnalyticsTracker("test") { event -> events.update { it + event } },
    )
    viewModel.open(BookDetailArgs(id = "unknown", title = "책"), accessToken = null)
    advanceUntilIdle()

    viewModel.createReview(ReviewCreateRequest("감상"))
    awaitReal { events.first { logged -> logged.any { it.name == "cc_action_result" } } }

    val result = events.value.last { it.name == "cc_action_result" }
    assertEquals(0, requestCount)
    assertEquals("unresolved", result.stringParameters["request_route"])
    assertEquals("target_resolution", result.stringParameters["failure_stage"])
    assertNull(result.longParameters["http_status"])
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
  fun `별점 기준을 바꾸는 동안 기존 비교 기록을 유지한다`() = runViewModelTest {
    val secondComparisonStarted = CompletableDeferred<Unit>()
    val finishSecondComparison = CompletableDeferred<Unit>()
    val engine = MockEngine { request ->
      when {
        request.url.encodedPath == "/api/v1/members/me/ratings/comparison" &&
          request.url.parameters["criterion"] == "4.0" -> respond(
          """{"lower":null,"current":{"bookId":1,"title":"기존 작품","myRating":4.0,"ratingUpdatedAt":"2026-08-01T00:00:00Z"},"higher":null}""",
          headers = jsonHeaders(),
        )
        request.url.encodedPath == "/api/v1/members/me/ratings/comparison" -> {
          secondComparisonStarted.complete(Unit)
          finishSecondComparison.await()
          respond(
            """{"lower":null,"current":{"bookId":2,"title":"새 작품","myRating":4.5,"ratingUpdatedAt":"2026-08-02T00:00:00Z"},"higher":null}""",
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

    viewModel.loadRatingComparison(Rating.ofScore(4.0f))
    awaitReal { viewModel.uiState.first { it.ratingComparison.singleOrNull()?.title == "기존 작품" } }
    viewModel.loadRatingComparison(Rating.ofScore(4.5f))
    awaitReal { secondComparisonStarted.await() }

    assertEquals(listOf("기존 작품"), viewModel.uiState.value.ratingComparison.map { it.title })

    finishSecondComparison.complete(Unit)
    awaitReal { viewModel.uiState.first { it.ratingComparison.singleOrNull()?.title == "새 작품" } }
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

  @Test
  fun `상세 준비 이벤트는 재시도와 새로고침에도 한 번만 기록한다`() = runViewModelTest {
    val events = MutableStateFlow<List<AnalyticsEvent>>(emptyList())
    val engine = MockEngine { request ->
      if (request.url.encodedPath.contains("/by-isbn/")) {
        respond(DETAIL_WITHOUT_RECORD, headers = jsonHeaders())
      } else {
        respond(EMPTY_REVIEWS, headers = jsonHeaders())
      }
    }
    val client = testClient(engine)
    val viewModel = viewModel(
      client,
      client,
      platform(readGuest = { null }),
      AnalyticsTracker("test") { event -> events.update { it + event } },
    )

    viewModel.open(book().copy(isbn13 = "9780000000042"), accessToken = null)
    awaitReal { while (events.value.count { it.name == "cc_load_result" } < 1) kotlinx.coroutines.yield() }
    viewModel.retry()
    awaitReal { while (events.value.count { it.name == "cc_load_result" } < 2) kotlinx.coroutines.yield() }

    assertEquals(1, events.value.count { it.name == "cc_book_detail_ready" })
    assertEquals(2, events.value.count { it.name == "cc_load_result" })
  }

  private fun viewModel(
    writeClient: HttpClient,
    authClient: HttpClient,
    callbacks: AuthPlatformCallbacks,
    analytics: AnalyticsTracker = AnalyticsTracker.None,
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
  ) = BookDetailViewModel(
    repository = BookDetailRemoteRepository(writeClient),
    libraryRepository = LibraryRemoteRepository(writeClient),
    authPlatform = callbacks,
    authRepository = MobileAuthRemoteRepository(authClient),
    analytics = analytics,
    savedStateHandle = savedStateHandle,
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
