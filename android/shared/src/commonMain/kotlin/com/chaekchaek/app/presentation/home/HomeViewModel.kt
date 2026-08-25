package com.chaekchaek.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.domain.feed.FeedRepository
import com.chaekchaek.app.domain.feed.FeedSection
import com.chaekchaek.app.domain.reader.GuestQuota
import com.chaekchaek.app.presentation.common.TimeLabels
import com.chaekchaek.app.presentation.common.toAppError
import com.chaekchaek.app.presentation.common.withDelayedApiLoading
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant

@Inject
class HomeViewModel(
    private val feedRepository: FeedRepository,
    private val clock: Clock,
) : ViewModel() {
    private var accessToken: String? = null
    private var hasObservedAuthentication = false
    private var loadJob: Job? = null
    private val _uiState = MutableStateFlow<HomeUiState>(
        HomeUiState.Content(emptyList(), guestBanner = null),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    fun authenticate(accessToken: String?) {
        if (!hasObservedAuthentication) {
            hasObservedAuthentication = true
            if (this.accessToken == accessToken) return
        }
        this.accessToken = accessToken
        load()
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val previousState = _uiState.value
            withDelayedApiLoading(
                onLoadingChanged = { loading ->
                    if (loading) {
                        _uiState.value = HomeUiState.Loading
                    } else if (_uiState.value == HomeUiState.Loading) {
                        _uiState.value = previousState
                    }
                },
            ) {
                _uiState.value = try {
                    val feed = feedRepository.homeFeed(accessToken)
                    val sections = feed.visibleSections()
                    if (feed.isEmpty()) {
                        HomeUiState.Empty
                    } else {
                        val now = clock.now()
                        // ponytail: 홈 피드가 더미인 동안 2/3 고정. 인증 연결 시 Viewer에서 매핑한다.
                        val quota = GuestQuota(viewed = 2)
                        HomeUiState.Content(
                            sections = sections.map { it.toUiModel(now) },
                            guestBanner = GuestBannerUiModel(
                                progressLabel = HomeLabels.guestProgress(quota.viewed, quota.limit),
                                exhausted = quota.isExhausted(),
                            ),
                            readingBook = feed.readingBook?.let { book ->
                                ReadingBookUiModel(
                                    bookId = book.bookId,
                                    isbn13 = book.isbn13,
                                    title = book.title,
                                    coverId = book.coverId,
                                    currentPage = book.currentPage,
                                    totalPages = book.totalPages,
                                )
                            },
                        )
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    HomeUiState.Failure(error.toAppError())
                }
            }
        }
    }
}

private fun FeedSection.toUiModel(now: Instant): FeedSectionUiModel = when (this) {
    is FeedSection.TrendingBooks -> FeedSectionUiModel.TrendingBooks(
        books = books.map { book ->
            TrendingBookUiModel(
                bookId = book.bookId,
                isbn13 = book.isbn13,
                title = book.title,
                coverId = book.coverId,
                statsLabel = HomeLabels.trendingStats(book.noteCount, book.replyCount),
            )
        },
        moreLabel = HomeLabels.trendingMore(totalCount),
    )

    is FeedSection.RecentQuotes -> FeedSectionUiModel.RecentQuotes(
        title = HomeLabels.RECENT_QUOTES_TITLE,
        cards = cards.map { card ->
            QuoteCardUiModel(
                noteId = card.noteId,
                bookId = card.bookId,
                isbn13 = card.isbn13,
                bookTitle = card.bookTitle,
                coverId = card.coverId,
                authorLabel = HomeLabels.author(
                    card.authorLabel,
                    TimeLabels.relative(card.createdAt, now),
                ),
                authorProfileImageUrl = card.authorProfileImageUrl,
                quoteText = card.quoteText,
                replyLabel = HomeLabels.quoteReply(card.replyCount),
            )
        },
    )

    is FeedSection.OverlappedBooks -> FeedSectionUiModel.OverlappedBooks(
        title = HomeLabels.OVERLAPPED_BOOKS_TITLE,
        cards = cards.map { card ->
            OverlappedCardUiModel(
                bookId = card.bookId,
                title = card.title,
                coverId = card.coverId,
                noteCountLabel = HomeLabels.noteCount(card.noteCount),
                authorLabel = HomeLabels.author(
                    card.authorLabel,
                    TimeLabels.relative(card.createdAt, now),
                ),
                excerpt = card.excerpt,
                replyLabel = HomeLabels.overlappedReply(card.replyCount),
            )
        },
    )
}
