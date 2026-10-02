package com.chaekchaek.app.ui.feed

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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
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
