package com.chaekchaek.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.domain.feed.FeedRepository
import com.chaekchaek.app.domain.feed.FeedSection
import com.chaekchaek.app.domain.reader.GuestQuota
import com.chaekchaek.app.presentation.common.TimeLabels
import com.chaekchaek.app.presentation.common.toAppError
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
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val sections = feedRepository.homeFeed().visibleSections()
                if (sections.isEmpty()) {
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

private fun FeedSection.toUiModel(now: Instant): FeedSectionUiModel = when (this) {
    is FeedSection.TrendingBooks -> FeedSectionUiModel.TrendingBooks(
        books = books.map { book ->
            TrendingBookUiModel(
                bookId = book.bookId,
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
                bookTitle = card.bookTitle,
                coverId = card.coverId,
                authorLabel = HomeLabels.author(
                    card.authorLabel,
                    TimeLabels.relative(card.createdAt, now),
                ),
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
