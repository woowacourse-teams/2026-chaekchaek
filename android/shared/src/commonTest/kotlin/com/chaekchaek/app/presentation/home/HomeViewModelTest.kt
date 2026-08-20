package com.chaekchaek.app.presentation.home

import com.chaekchaek.app.domain.book.BookId
import com.chaekchaek.app.domain.feed.FeedRepository
import com.chaekchaek.app.domain.feed.FeedSection
import com.chaekchaek.app.domain.feed.HomeFeed
import com.chaekchaek.app.domain.feed.ReadingBook
import com.chaekchaek.app.domain.feed.TrendingBook
import com.chaekchaek.app.presentation.common.AppError
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Instant

private val FIXED_NOW = Instant.parse("2026-08-12T12:00:00Z")
private val FIXED_CLOCK = object : Clock {
    override fun now(): Instant = FIXED_NOW
}

private fun contentFeed(): HomeFeed = HomeFeed(
    listOf(
        FeedSection.TrendingBooks(
            books = listOf(
                TrendingBook(
                    bookId = BookId("bk_001"),
                    title = "보이지 않는 도시",
                    coverId = "cover-01",
                    noteCount = 128,
                    replyCount = 46,
                ),
            ),
            totalCount = 12,
        ),
    ),
)

private class TestFeedRepository(
    var feed: HomeFeed = contentFeed(),
    var failure: Throwable? = null,
) : FeedRepository {
    var callCount: Int = 0

    var accessToken: String? = null

    override suspend fun homeFeed(accessToken: String?): HomeFeed {
        callCount += 1
        this.accessToken = accessToken
        failure?.let { throw it }
        return feed
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
        block()
    } finally {
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @Test
    fun `처음에는 로딩 상태다`() = runViewModelTest {
        // given & when : ViewModel 을 생성하면
        val viewModel = HomeViewModel(TestFeedRepository(), FIXED_CLOCK)

        // then : 저장소 응답 전에는 로딩 상태다
        viewModel.uiState.value shouldBe HomeUiState.Loading
        advanceUntilIdle()
    }

    @Test
    fun `로드에 성공하면 섹션을 화면 모델로 바꾼다`() = runViewModelTest {
        // given : 내용이 있는 홈 피드 저장소가 있다
        val viewModel = HomeViewModel(TestFeedRepository(), FIXED_CLOCK)

        // when : 최초 로드를 마치면
        advanceUntilIdle()

        // then : 섹션 하나를 담은 Content 상태가 된다
        val content = viewModel.uiState.value.shouldBeInstanceOf<HomeUiState.Content>()
        content.sections.size shouldBe 1
        content.guestBanner shouldBe GuestBannerUiModel("지금 2 / 3", exhausted = false)
    }

    @Test
    fun `빈 피드는 빈 상태가 된다`() = runViewModelTest {
        // given : 섹션이 없는 홈 피드 저장소가 있다
        val viewModel = HomeViewModel(TestFeedRepository(feed = HomeFeed(emptyList())), FIXED_CLOCK)

        // when : 최초 로드를 마치면
        advanceUntilIdle()

        // then : 빈 상태가 된다
        viewModel.uiState.value shouldBe HomeUiState.Empty
    }

    @Test
    fun `로드가 실패하면 오류 상태가 된다`() = runViewModelTest {
        // given : 로드에 실패하는 저장소가 있다
        val repository = TestFeedRepository(failure = IllegalStateException("failed"))
        val viewModel = HomeViewModel(repository, FIXED_CLOCK)

        // when : 최초 로드를 마치면
        advanceUntilIdle()

        // then : 현재는 알 수 없는 오류로 표시한다
        viewModel.uiState.value shouldBe HomeUiState.Failure(AppError.Unknown)
    }

    @Test
    fun `실패 후 재시도하면 다시 로드한다`() = runViewModelTest {
        // given : 첫 로드에 실패한 저장소가 있다
        val repository = TestFeedRepository(failure = IllegalStateException("failed"))
        val viewModel = HomeViewModel(repository, FIXED_CLOCK)
        advanceUntilIdle()
        repository.failure = null

        // when : 재시도하면
        viewModel.retry()

        // then : 로딩을 거쳐 성공하고 저장소를 두 번 호출한다
        viewModel.uiState.value shouldBe HomeUiState.Loading
        advanceUntilIdle()
        viewModel.uiState.value.shouldBeInstanceOf<HomeUiState.Content>()
        repository.callCount shouldBe 2
    }

    @Test
    fun `인증되면 읽는 중인 책을 다시 불러온다`() = runViewModelTest {
        val repository = TestFeedRepository(
            feed = HomeFeed(
                sections = emptyList(),
                readingBook = ReadingBook(
                    bookId = BookId("42"),
                    isbn13 = "9780000000042",
                    title = "역병",
                    coverId = "cover-42",
                    currentPage = 132,
                    totalPages = 320,
                ),
            ),
        )
        val viewModel = HomeViewModel(repository, FIXED_CLOCK)
        advanceUntilIdle()

        viewModel.authenticate("access-token")
        advanceUntilIdle()

        repository.accessToken shouldBe "access-token"
        viewModel.uiState.value.shouldBeInstanceOf<HomeUiState.Content>().readingBook shouldBe
            ReadingBookUiModel(
                bookId = BookId("42"),
                isbn13 = "9780000000042",
                title = "역병",
                coverId = "cover-42",
                currentPage = 132,
                totalPages = 320,
            )
    }
}
