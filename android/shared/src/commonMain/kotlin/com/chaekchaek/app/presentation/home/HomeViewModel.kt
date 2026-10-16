package com.chaekchaek.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.analytics.AnalyticsTracker
import com.chaekchaek.app.analytics.analyticsErrorCategory
import com.chaekchaek.app.domain.feed.FeedRepository
import com.chaekchaek.app.domain.feed.FeedSection
import com.chaekchaek.app.presentation.common.RefreshUiState
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
    private val analytics: AnalyticsTracker = AnalyticsTracker.None,
) : ViewModel() {
    private var accessToken: String? = null
    private var hasObservedAuthentication = false
    private var loadJob: Job? = null
    private var loadRevision = 0L
    private val _refreshState = MutableStateFlow(RefreshUiState())
    val refreshState: StateFlow<RefreshUiState> = _refreshState.asStateFlow()
    private val _uiState = MutableStateFlow<HomeUiState>(
        HomeUiState.Content(emptyList()),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    fun refresh() {
        if (loadJob?.isActive == true) return
        load(retainContent = true)
    }

    fun refresh(accessToken: String?) {
        if (this.accessToken != accessToken) {
            authenticate(accessToken)
            return
        }
        refresh()
    }

    fun authenticate(accessToken: String?) {
        if (!hasObservedAuthentication) {
            hasObservedAuthentication = true
            if (this.accessToken == accessToken) return
        }
        this.accessToken = accessToken
        load()
    }

    private fun load(retainContent: Boolean = false) {
        val revision = ++loadRevision
        val token = accessToken
        loadJob?.cancel()
        _refreshState.value = RefreshUiState(isRefreshing = retainContent)
        loadJob = viewModelScope.launch {
            val started = kotlin.time.TimeSource.Monotonic.markNow()
            val previousState = _uiState.value
            withDelayedApiLoading(
                onLoadingChanged = { loading ->
                    if (revision != loadRevision) return@withDelayedApiLoading
                    if (retainContent) {
                        _refreshState.value = _refreshState.value.copy(showLoading = loading)
                    } else if (loading) {
                        _uiState.value = HomeUiState.Loading
                    } else if (_uiState.value == HomeUiState.Loading) {
                        _uiState.value = previousState
                    }
                },
            ) {
                val result = try {
                    val feed = feedRepository.homeFeed(token)
                    val sections = feed.visibleSections()
                    if (feed.isEmpty()) {
                        analytics.log(
                            name = "cc_load_result",
                            strings = mapOf("resource" to "home_feed", "outcome" to "empty"),
                            longs = mapOf("duration_ms" to started.elapsedNow().inWholeMilliseconds),
                        )
                        HomeUiState.Empty
                    } else {
                        analytics.log(
                            name = "cc_load_result",
                            strings = mapOf("resource" to "home_feed", "outcome" to "success"),
                            longs = mapOf(
                                "duration_ms" to started.elapsedNow().inWholeMilliseconds,
                                "result_count" to sections.size.toLong(),
                            ),
                        )
                        val now = clock.now()
                        HomeUiState.Content(
                            sections = sections.map { it.toUiModel(now) },
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
                    analytics.log(
                        name = "cc_load_result",
                        strings = mapOf(
                            "resource" to "home_feed",
                            "outcome" to "failure",
                            "error_category" to analyticsErrorCategory(error),
                        ),
                        longs = mapOf("duration_ms" to started.elapsedNow().inWholeMilliseconds),
                    )
                    if (retainContent) {
                        if (revision == loadRevision) {
                            _refreshState.value = _refreshState.value.copy(errorMessage = "새로고침하지 못했어요")
                        }
                        previousState
                    } else HomeUiState.Failure(error.toAppError())
                }
                if (revision == loadRevision) _uiState.value = result
            }
            if (revision == loadRevision) {
                _refreshState.value = _refreshState.value.copy(isRefreshing = false, showLoading = false)
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
                noteCountLabel = HomeLabels.trendingNoteCount(book.noteCount),
                replyCountLabel = HomeLabels.trendingReplyCount(book.replyCount),
            )
        },
        moreLabel = HomeLabels.trendingMore(totalCount),
    )

    is FeedSection.RecentQuotes -> FeedSectionUiModel.RecentQuotes(
        title = HomeLabels.RECENT_QUOTES_TITLE,
        cards = cards.map { card ->
            QuoteCardUiModel(
                reviewId = card.reviewId,
                noteId = card.noteId,
                bookId = card.bookId,
                isbn13 = card.isbn13,
                bookTitle = card.bookTitle,
                coverId = card.coverId,
                authorName = card.authorLabel,
                timeLabel = TimeLabels.relative(card.createdAt, now),
                quoteText = card.quoteText,
                quote = card.quote,
                likeCount = card.likeCount,
                likedByMe = card.likedByMe,
                replyCount = card.replyCount,
                replyLabel = HomeLabels.quoteReply(card.replyCount),
                isSpoiler = card.isSpoiler,
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
                authorName = card.authorLabel,
                timeLabel = TimeLabels.relative(card.createdAt, now),
                excerpt = card.excerpt,
                replyLabel = HomeLabels.overlappedReply(card.replyCount),
            )
        },
    )
}
