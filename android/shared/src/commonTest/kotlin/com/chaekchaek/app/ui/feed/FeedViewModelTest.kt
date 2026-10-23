package com.chaekchaek.app.ui.feed

import androidx.lifecycle.SavedStateHandle
import com.chaekchaek.app.domain.book.BookId
import com.chaekchaek.app.domain.feed.FeedReviewActions
import com.chaekchaek.app.domain.feed.FeedViewer
import com.chaekchaek.app.domain.feed.QuoteCard
import com.chaekchaek.app.domain.feed.ReviewFeedPage
import com.chaekchaek.app.domain.feed.ReviewFeedRepository
import com.chaekchaek.app.domain.note.NoteId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Clock
import kotlin.time.Instant

private val FEED_NOW = Instant.parse("2026-10-01T08:00:00Z")
private val FEED_CLOCK = object : Clock {
    override fun now(): Instant = FEED_NOW
}

private fun review(
    id: Long,
    spoiler: Boolean = false,
    likeCount: Int = 0,
    likedByMe: Boolean = false,
) = QuoteCard(
    reviewId = id,
    noteId = NoteId("review-$id"),
    bookId = BookId(id.toString()),
    isbn13 = "9780000000$id",
    bookTitle = "책 $id",
    coverId = "cover-$id",
    authorLabel = "독자 $id",
    createdAt = FEED_NOW,
    quoteText = "감상 $id",
    replyCount = 0,
    likeCount = likeCount,
    likedByMe = likedByMe,
    isSpoiler = spoiler,
)

private class TestReviewFeedRepository(
    private val pages: Map<Int, ReviewFeedPage>,
) : ReviewFeedRepository {
    val requestedPages = mutableListOf<Int>()
    val requestedViewers = mutableListOf<FeedViewer>()
    var failurePage: Int? = null

    override suspend fun reviewFeed(page: Int, viewer: FeedViewer): ReviewFeedPage {
        requestedPages += page
        requestedViewers += viewer
        if (failurePage == page) error("failed")
        return checkNotNull(pages[page])
    }
}

private object TestFeedReviewActions : FeedReviewActions {
    override suspend fun toggleLike(reviewId: Long, likedByMe: Boolean, accessToken: String?) = Unit
    override suspend fun createReply(reviewId: Long, content: String, accessToken: String?) = Unit
}

private class LateCompletingFeedReviewActions : FeedReviewActions {
    private var continuation: Continuation<Unit>? = null

    override suspend fun toggleLike(reviewId: Long, likedByMe: Boolean, accessToken: String?) {
        suspendCoroutine { continuation = it }
    }

    override suspend fun createReply(reviewId: Long, content: String, accessToken: String?) = Unit

    fun completeToggle() {
        checkNotNull(continuation).resume(Unit)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun runFeedViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
        block()
    } finally {
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {
    @Test
    fun `건너뛴 실제 페이지까지 최초 조회와 추가 로딩 후 복원한다`() = runFeedViewModelTest {
        for (skipInitial in listOf(true, false)) {
            var changed = false
            val requests = mutableListOf<Int>()
            val repository = object : ReviewFeedRepository {
                override suspend fun reviewFeed(page: Int, viewer: FeedViewer): ReviewFeedPage {
                    requests += page
                    val actual = if (!changed && page == (if (skipInitial) 1 else 2)) 3 else page
                    return ReviewFeedPage(listOf(review(actual.toLong())), 3,
                        if (actual < 3) actual + 1 else null, lastLoadedPage = actual)
                }
            }
            val saved = SavedStateHandle()
            val vm = FeedViewModel(repository, FEED_CLOCK, TestFeedReviewActions, savedStateHandle = saved)
            advanceUntilIdle()
            if (!skipInitial) { vm.loadMore(); advanceUntilIdle() }
            saved.get<Int>(FeedSavedStateKeys.LoadedPage) shouldBe 3
            changed = true
            requests.clear()
            val restored = FeedViewModel(repository, FEED_CLOCK, TestFeedReviewActions, savedStateHandle = saved)
            advanceUntilIdle()
            requests shouldBe listOf(1, 2, 3)
            restored.uiState.value.shouldBeInstanceOf<FeedUiState.Content>()
                .reviews.map { it.reviewId } shouldBe listOf(1L, 2L, 3L)
        }
    }

    @Test
    fun `최초 조회 오류창에서 복구한 요청은 재시도로 기록한다`() = runFeedViewModelTest {
        assertErrorRecoveryRecordsRetry(refresh = false)
    }

    @Test
    fun `새로고침 오류창에서 복구한 요청은 재시도로 기록하고 내용을 유지한다`() = runFeedViewModelTest {
        assertErrorRecoveryRecordsRetry(refresh = true)
    }

    private suspend fun TestScope.assertErrorRecoveryRecordsRetry(refresh: Boolean) {
        val events = mutableListOf<com.chaekchaek.app.analytics.AnalyticsEvent>()
        val analytics = com.chaekchaek.app.analytics.AnalyticsTracker("test", events::add)
        val repository = TestReviewFeedRepository(mapOf(1 to ReviewFeedPage(listOf(review(1)), 1, null)))
        if (!refresh) repository.failurePage = 1
        val vm = FeedViewModel(repository, FEED_CLOCK, TestFeedReviewActions, analytics = analytics)
        advanceUntilIdle()
        if (refresh) {
            repository.failurePage = 1
            vm.refresh()
            advanceUntilIdle()
        }
        val failed = events.last { it.name == "cc_load_result" }
        failed.stringParameters["trigger"] shouldBe if (refresh) "refresh" else "initial"
        failed.stringParameters["outcome"] shouldBe "failure"
        repository.failurePage = null
        val notice = checkNotNull(vm.errors.notice.value)
        checkNotNull(vm.errors.takeRecovery(notice.id)).invoke(null)
        advanceUntilIdle()

        val recovered = events.last { it.name == "cc_load_result" }
        recovered.stringParameters["trigger"] shouldBe "retry"
        recovered.stringParameters["outcome"] shouldBe "success"
        vm.uiState.value.shouldBeInstanceOf<FeedUiState.Content>().reviews.single().reviewId shouldBe 1L
        vm.errors.notice.value shouldBe null
    }

    @Test
    fun `갱신 중 성공한 좋아요는 늦은 갱신 성공에 덮이지 않는다`() = runFeedViewModelTest {
        assertRefreshKeepsAcceptedWrite(reply = false, refreshFails = false)
    }

    @Test
    fun `갱신 중 성공한 좋아요는 늦은 갱신 실패에 덮이지 않는다`() = runFeedViewModelTest {
        assertRefreshKeepsAcceptedWrite(reply = false, refreshFails = true)
    }

    @Test
    fun `갱신 중 제출한 답글과 완료 콜백은 늦은 갱신 성공 뒤에도 유지된다`() = runFeedViewModelTest {
        assertRefreshKeepsAcceptedWrite(reply = true, refreshFails = false)
    }

    @Test
    fun `갱신 중 제출한 답글과 완료 콜백은 늦은 갱신 실패 뒤에도 유지된다`() = runFeedViewModelTest {
        assertRefreshKeepsAcceptedWrite(reply = true, refreshFails = true)
    }

    @Test
    fun `갱신을 중단한 좋아요도 로그아웃 뒤의 피드 상태를 바꾸지 않는다`() = runFeedViewModelTest {
        val pendingRefresh = CompletableDeferred<ReviewFeedPage>()
        val original = ReviewFeedPage(listOf(review(1)), 1, null)
        var requests = 0
        val repository = object : ReviewFeedRepository {
            override suspend fun reviewFeed(page: Int, viewer: FeedViewer): ReviewFeedPage {
                requests++
                return if (requests == 2) withContext(NonCancellable) { pendingRefresh.await() } else original
            }
        }
        val actions = LateCompletingFeedReviewActions()
        val viewModel = FeedViewModel(repository, FEED_CLOCK, actions, initialAccessToken = "member-token")
        advanceUntilIdle()
        viewModel.refresh()
        runCurrent()
        viewModel.toggleReviewLike(1, false)
        runCurrent()

        viewModel.authenticate(null)
        actions.completeToggle()
        pendingRefresh.complete(original.copy(reviews = listOf(review(1, likedByMe = true, likeCount = 1))))
        advanceUntilIdle()

        val content = viewModel.uiState.value.shouldBeInstanceOf<FeedUiState.Content>()
        content.reviews.single().likedByMe shouldBe false
        content.reviews.single().likeCount shouldBe 0
        requests shouldBe 3
        viewModel.refreshState.value.isRefreshing shouldBe false
    }

    private suspend fun TestScope.assertRefreshKeepsAcceptedWrite(reply: Boolean, refreshFails: Boolean) {
        val pendingRefresh = CompletableDeferred<ReviewFeedPage>()
        val original = ReviewFeedPage(listOf(review(1), review(2)), 2, null)
        var requests = 0
        val repository = object : ReviewFeedRepository {
            override suspend fun reviewFeed(page: Int, viewer: FeedViewer): ReviewFeedPage {
                requests++
                return if (requests == 1) original else withContext(NonCancellable) { pendingRefresh.await() }
            }
        }
        val writes = mutableListOf<String>()
        val actions = object : FeedReviewActions {
            override suspend fun toggleLike(reviewId: Long, likedByMe: Boolean, accessToken: String?) {
                writes += "like:$reviewId:$likedByMe"
            }
            override suspend fun createReply(reviewId: Long, content: String, accessToken: String?) {
                writes += "reply:$reviewId:$content"
            }
        }
        val viewModel = FeedViewModel(repository, FEED_CLOCK, actions)
        advanceUntilIdle()
        viewModel.refresh()
        runCurrent()
        var savedReplies = 0
        if (reply) viewModel.createReply(1, "작성한 답글") { savedReplies++ }
        else viewModel.toggleReviewLike(1, false)
        runCurrent()
        writes shouldBe listOf(if (reply) "reply:1:작성한 답글" else "like:1:false")
        val acceptedContent = viewModel.uiState.value.shouldBeInstanceOf<FeedUiState.Content>()
        acceptedContent.reviews.first().let {
            it.likedByMe shouldBe !reply
            it.likeCount shouldBe if (reply) 0 else 1
            it.replyCount shouldBe if (reply) 1 else 0
        }

        if (refreshFails) pendingRefresh.completeExceptionally(IllegalStateException("refresh failed"))
        else pendingRefresh.complete(original)
        advanceUntilIdle()

        viewModel.uiState.value shouldBe acceptedContent
        viewModel.uiState.value.shouldBeInstanceOf<FeedUiState.Content>().reviews.first().let {
            it.likedByMe shouldBe !reply
            it.likeCount shouldBe if (reply) 0 else 1
            it.replyCount shouldBe if (reply) 1 else 0
        }
        writes.size shouldBe 1
        savedReplies shouldBe if (reply) 1 else 0
        requests shouldBe 2
        viewModel.refreshState.value.isRefreshing shouldBe false
        viewModel.refreshState.value.errorMessage shouldBe null
    }

    @Test
    fun `갱신은 읽은 구간을 재조회하고 실패하면 기존 페이지와 내용을 유지한다`() = runFeedViewModelTest {
        val repository = TestReviewFeedRepository(mapOf(
            1 to ReviewFeedPage(listOf(review(1)), 2, 2),
            2 to ReviewFeedPage(listOf(review(2)), 2, null),
        ))
        val saved = SavedStateHandle()
        val viewModel = FeedViewModel(repository, FEED_CLOCK, TestFeedReviewActions, savedStateHandle = saved)
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()
        val previous = viewModel.uiState.value
        repository.failurePage = 2
        viewModel.refresh()
        viewModel.refresh()
        advanceUntilIdle()
        viewModel.uiState.value shouldBe previous
        (viewModel.errors.notice.value != null) shouldBe true
        saved.get<Int>(FeedSavedStateKeys.LoadedPage) shouldBe 2
        repository.failurePage = null
        viewModel.errors.takeRecovery(viewModel.errors.notice.value!!.id)?.invoke(null)
        advanceUntilIdle()
        repository.requestedPages shouldBe listOf(1, 2, 1, 2, 1, 2)
        viewModel.uiState.value.shouldBeInstanceOf<FeedUiState.Content>().reviews.size shouldBe 2
    }

    @Test
    fun `추가 조회 실패는 자동 반복하지 않고 명시적으로 재시도한다`() = runFeedViewModelTest {
        val repository = TestReviewFeedRepository(mapOf(
            1 to ReviewFeedPage(listOf(review(1)), 2, 2),
            2 to ReviewFeedPage(listOf(review(2)), 2, null),
        ))
        val viewModel = FeedViewModel(repository, FEED_CLOCK, TestFeedReviewActions)
        advanceUntilIdle()
        repository.failurePage = 2
        viewModel.loadMore()
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()
        repository.requestedPages shouldBe listOf(1, 2)
        viewModel.createReply(null, "요청")
        viewModel.clearRequestError()
        viewModel.loadMore()
        advanceUntilIdle()
        repository.requestedPages shouldBe listOf(1, 2)
        viewModel.uiState.value.shouldBeInstanceOf<FeedUiState.Content>().loadMoreFailed shouldBe true
        repository.failurePage = null
        viewModel.retryLoadMore()
        advanceUntilIdle()
        repository.requestedPages shouldBe listOf(1, 2, 2)
        viewModel.uiState.value.shouldBeInstanceOf<FeedUiState.Content>().nextPage shouldBe null
    }

    @Test
    fun `첫 피드가 지연되면 빈 자리 대신 로딩을 표시한다`() = runFeedViewModelTest {
        val response = CompletableDeferred<ReviewFeedPage>()
        val repository = object : ReviewFeedRepository {
            override suspend fun reviewFeed(page: Int, viewer: FeedViewer) = response.await()
        }
        val vm = FeedViewModel(repository, FEED_CLOCK, TestFeedReviewActions)
        runCurrent()
        advanceTimeBy(501)
        runCurrent()
        vm.uiState.value shouldBe FeedUiState.Loading
        response.complete(ReviewFeedPage(listOf(review(1)), 1, null))
        advanceUntilIdle()
        vm.uiState.value.shouldBeInstanceOf<FeedUiState.Content>().reviews.size shouldBe 1
    }

    @Test
    fun `토큰 갱신 중에는 목록을 유지하고 갱신 실패 후에도 다음 페이지를 읽을 수 있다`() = runFeedViewModelTest {
        val refresh = CompletableDeferred<ReviewFeedPage>()
        val pagination = CompletableDeferred<ReviewFeedPage>()
        var calls = 0
        val repository = object : ReviewFeedRepository {
            override suspend fun reviewFeed(page: Int, viewer: FeedViewer): ReviewFeedPage {
                calls++
                if (page == 2 && viewer == FeedViewer.Member("before")) return pagination.await()
                if (page == 1 && viewer == FeedViewer.Member("after")) return refresh.await()
                return ReviewFeedPage(listOf(review(page.toLong())), 2, if (page == 1) 2 else null)
            }
        }
        val viewModel = FeedViewModel(repository, FEED_CLOCK, TestFeedReviewActions, initialAccessToken = "before")
        advanceUntilIdle()
        viewModel.loadMore()
        runCurrent()
        viewModel.authenticate("after")
        advanceUntilIdle()
        viewModel.uiState.value.shouldBeInstanceOf<FeedUiState.Content>().loadingMore shouldBe false
        viewModel.loadMore()
        runCurrent()
        calls shouldBe 3
        refresh.completeExceptionally(IllegalStateException("refresh failed"))
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()
        viewModel.uiState.value.shouldBeInstanceOf<FeedUiState.Content>().reviews.size shouldBe 2
    }

    @Test
    fun `초기 회원 인증을 기다리는 피드는 조회와 쓰기를 보류하고 저장 페이지를 복원한다`() = runFeedViewModelTest {
        val repository = TestReviewFeedRepository(mapOf(
            1 to ReviewFeedPage(listOf(review(1)), 2, 2),
            2 to ReviewFeedPage(listOf(review(2)), 2, null),
        ))
        var writes = 0
        val actions = object : FeedReviewActions {
            override suspend fun toggleLike(reviewId: Long, likedByMe: Boolean, accessToken: String?) { writes++ }
            override suspend fun createReply(reviewId: Long, content: String, accessToken: String?) { writes++ }
        }
        val savedState = SavedStateHandle(mapOf(FeedSavedStateKeys.LoadedPage to 2))
        val viewModel = FeedViewModel(repository, FEED_CLOCK, actions,
            savedStateHandle = savedState, deferInitialLoad = true)
        runCurrent()
        viewModel.createReply(1, "회원 초안")
        viewModel.toggleReviewLike(1, false)
        runCurrent()
        writes shouldBe 0
        repository.requestedPages shouldBe emptyList()
        savedState.get<Int>(FeedSavedStateKeys.LoadedPage) shouldBe 2

        viewModel.authenticate("restored-member")
        advanceUntilIdle()
        repository.requestedPages shouldBe listOf(1, 2)
        repository.requestedViewers.distinct() shouldBe listOf(FeedViewer.Member("restored-member"))
        savedState.get<Int>(FeedSavedStateKeys.LoadedPage) shouldBe 2
    }

    @Test
    fun `같은 회원의 토큰 갱신은 읽던 페이지를 유지하고 로그아웃은 초기화한다`() = runFeedViewModelTest {
        val repository = TestReviewFeedRepository(mapOf(
            1 to ReviewFeedPage(listOf(review(1)), 2, 2),
            2 to ReviewFeedPage(listOf(review(2)), 2, null),
        ))
        val savedState = SavedStateHandle()
        val viewModel = FeedViewModel(repository, FEED_CLOCK, TestFeedReviewActions, savedStateHandle = savedState)
        viewModel.authenticate("member-before-renewal")
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()

        viewModel.authenticate("member-after-renewal")
        advanceUntilIdle()

        savedState.get<Int>(FeedSavedStateKeys.LoadedPage) shouldBe 2
        viewModel.uiState.value.shouldBeInstanceOf<FeedUiState.Content>().reviews.size shouldBe 2
        repository.requestedViewers.last() shouldBe FeedViewer.Member("member-after-renewal")

        viewModel.authenticate(null)
        advanceUntilIdle()
        savedState.get<Int>(FeedSavedStateKeys.LoadedPage) shouldBe 1
    }

    @Test
    fun `복원은 첫 페이지부터 읽던 페이지까지 다시 조회하고 중복 감상을 제거한다`() = runFeedViewModelTest {
        val repository = TestReviewFeedRepository(mapOf(
            1 to ReviewFeedPage(listOf(review(1)), 2, 2),
            2 to ReviewFeedPage(listOf(review(1), review(2)), 2, null),
        ))
        val savedState = SavedStateHandle(mapOf(FeedSavedStateKeys.LoadedPage to 2))
        val viewModel = FeedViewModel(repository, FEED_CLOCK, TestFeedReviewActions, savedStateHandle = savedState)
        advanceUntilIdle()

        repository.requestedPages shouldBe listOf(1, 2)
        viewModel.uiState.value.shouldBeInstanceOf<FeedUiState.Content>()
            .reviews.map { it.reviewId } shouldBe listOf(1L, 2L)

        viewModel.authenticate("different-member")
        advanceUntilIdle()
        repository.requestedPages shouldBe listOf(1, 2, 1)
        savedState.get<Int>(FeedSavedStateKeys.LoadedPage) shouldBe 1
    }

    @Test
    fun `복원 조회 실패는 자동 반복하지 않고 명시적 재시도로 복구한다`() = runFeedViewModelTest {
        val repository = TestReviewFeedRepository(mapOf(
            1 to ReviewFeedPage(listOf(review(1)), 2, 2),
            2 to ReviewFeedPage(listOf(review(2)), 2, null),
        )).apply { failurePage = 2 }
        val savedState = SavedStateHandle(mapOf(FeedSavedStateKeys.LoadedPage to 2))
        val viewModel = FeedViewModel(repository, FEED_CLOCK, TestFeedReviewActions, savedStateHandle = savedState)
        advanceUntilIdle()
        viewModel.uiState.value.shouldBeInstanceOf<FeedUiState.Content>()
        viewModel.errors.notice.value?.action shouldBe com.chaekchaek.app.presentation.common.ErrorAction.Retry
        repository.requestedPages shouldBe listOf(1, 2)

        repository.failurePage = null
        viewModel.errors.takeRecovery(viewModel.errors.notice.value!!.id)?.invoke(null)
        advanceUntilIdle()
        repository.requestedPages shouldBe listOf(1, 2, 1, 2)
        viewModel.uiState.value.shouldBeInstanceOf<FeedUiState.Content>().reviews.size shouldBe 2
    }

    @Test
    fun `목록 끝에서 다음 페이지를 기존 감상 아래에 이어 붙인다`() = runFeedViewModelTest {
        val repository = TestReviewFeedRepository(
            pages = mapOf(
                1 to ReviewFeedPage(listOf(review(1)), totalCount = 2, nextPage = 2),
                2 to ReviewFeedPage(listOf(review(2, spoiler = true)), totalCount = 2, nextPage = null),
            ),
        )
        val viewModel = FeedViewModel(repository, FEED_CLOCK, TestFeedReviewActions)
        advanceUntilIdle()

        viewModel.loadMore()
        viewModel.loadMore()
        advanceUntilIdle()

        val content = viewModel.uiState.value.shouldBeInstanceOf<FeedUiState.Content>()
        content.reviews.map { it.reviewId } shouldBe listOf(1L, 2L)
        content.reviews.last().isSpoiler shouldBe true
        content.nextPage shouldBe null
        repository.requestedPages shouldBe listOf(1, 2)
    }

    @Test
    fun `다음 페이지 실패 시 기존 감상을 유지하고 재시도 오류를 표시한다`() = runFeedViewModelTest {
        val repository = TestReviewFeedRepository(
            pages = mapOf(1 to ReviewFeedPage(listOf(review(1)), totalCount = 2, nextPage = 2)),
        ).apply { failurePage = 2 }
        val viewModel = FeedViewModel(repository, FEED_CLOCK, TestFeedReviewActions)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val content = viewModel.uiState.value.shouldBeInstanceOf<FeedUiState.Content>()
        content.reviews.map { it.reviewId } shouldBe listOf(1L)
        content.nextPage shouldBe 2
        content.loadingMore shouldBe false
        content.requestError shouldBe FeedRequestError.LoadMore
    }

    @Test
    fun `다음 페이지 실패 후 명시적으로 같은 페이지를 재시도한다`() = runFeedViewModelTest {
        val repository = TestReviewFeedRepository(
            pages = mapOf(
                1 to ReviewFeedPage(listOf(review(1)), totalCount = 2, nextPage = 2),
                2 to ReviewFeedPage(listOf(review(2)), totalCount = 2, nextPage = null),
            ),
        ).apply { failurePage = 2 }
        val viewModel = FeedViewModel(repository, FEED_CLOCK, TestFeedReviewActions)
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()

        repository.failurePage = null
        viewModel.retryLoadMore()
        advanceUntilIdle()

        val content = viewModel.uiState.value.shouldBeInstanceOf<FeedUiState.Content>()
        content.reviews.map { it.reviewId } shouldBe listOf(1L, 2L)
        repository.requestedPages shouldBe listOf(1, 2, 2)
    }

    @Test
    fun `비회원 피드 재조회에 저장된 게스트 토큰을 사용한다`() = runFeedViewModelTest {
        val repository = TestReviewFeedRepository(
            pages = mapOf(1 to ReviewFeedPage(listOf(review(1)), totalCount = 1, nextPage = null)),
        )
        val viewModel = FeedViewModel(
            repository = repository,
            clock = FEED_CLOCK,
            reviewActions = TestFeedReviewActions,
            readGuestToken = { "guest-token" },
        )
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()

        repository.requestedViewers shouldBe listOf(
            FeedViewer.Guest("guest-token"),
            FeedViewer.Guest("guest-token"),
        )
    }

    @Test
    fun `로그인 전에 시작한 좋아요 응답은 회원 피드 상태를 바꾸지 않는다`() = runFeedViewModelTest {
        val repository = TestReviewFeedRepository(
            pages = mapOf(
                1 to ReviewFeedPage(
                    reviews = listOf(review(1, likeCount = 0, likedByMe = false)),
                    totalCount = 1,
                    nextPage = null,
                ),
            ),
        )
        val actions = LateCompletingFeedReviewActions()
        val viewModel = FeedViewModel(repository, FEED_CLOCK, actions)
        advanceUntilIdle()

        viewModel.toggleReviewLike(reviewId = 1, likedByMe = false)
        runCurrent()
        viewModel.authenticate("member-token")
        actions.completeToggle()
        advanceUntilIdle()

        val review = viewModel.uiState.value.shouldBeInstanceOf<FeedUiState.Content>().reviews.single()
        review.likedByMe shouldBe false
        review.likeCount shouldBe 0
        repository.requestedViewers.last() shouldBe FeedViewer.Member("member-token")
    }
}
