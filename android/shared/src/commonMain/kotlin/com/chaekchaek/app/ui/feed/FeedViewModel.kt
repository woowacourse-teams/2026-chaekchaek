package com.chaekchaek.app.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.domain.feed.FeedReviewActions
import com.chaekchaek.app.domain.feed.QuoteCard
import com.chaekchaek.app.domain.feed.ReviewFeedRepository
import com.chaekchaek.app.presentation.common.TimeLabels
import com.chaekchaek.app.presentation.common.toAppError
import com.chaekchaek.app.presentation.common.withDelayedApiLoading
import com.chaekchaek.app.presentation.home.HomeLabels
import com.chaekchaek.app.presentation.home.QuoteCardUiModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

class FeedViewModel(
    private val repository: ReviewFeedRepository,
    private val clock: Clock,
    private val reviewActions: FeedReviewActions,
) : ViewModel() {
    private var accessToken: String? = null
    private var hasObservedAuthentication = false
    private var initialLoadJob: Job? = null
    private var loadMoreJob: Job? = null
    private var mutationJob: Job? = null
    private val _uiState = MutableStateFlow<FeedUiState>(
        FeedUiState.Content(emptyList(), totalCount = 0, nextPage = null),
    )
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        loadFirstPage()
    }

    fun retry() {
        loadFirstPage()
    }

    fun authenticate(accessToken: String?) {
        if (!hasObservedAuthentication) {
            hasObservedAuthentication = true
            if (this.accessToken == accessToken) return
        }
        this.accessToken = accessToken
        loadFirstPage()
    }

    fun loadMore() {
        val current = _uiState.value as? FeedUiState.Content ?: return
        val page = current.nextPage ?: return
        if (current.loadingMore || loadMoreJob?.isActive == true) return
        _uiState.value = current.copy(loadingMore = true, requestError = null)
        loadMoreJob = viewModelScope.launch {
            try {
                val next = repository.reviewFeed(page, accessToken)
                val latest = _uiState.value as? FeedUiState.Content ?: return@launch
                if (latest.nextPage != page) return@launch
                _uiState.value = latest.copy(
                    reviews = (latest.reviews + next.reviews.map { it.toUiModel() })
                        .distinctBy { it.noteId.value },
                    totalCount = next.totalCount,
                    nextPage = next.nextPage,
                    loadingMore = false,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                updateContent {
                    it.copy(
                        loadingMore = false,
                        requestError = "다음 감상을 불러오지 못했어요. 다시 시도해 주세요.",
                    )
                }
            }
        }
    }

    fun clearRequestError() {
        updateContent { it.copy(requestError = null) }
    }

    fun toggleReviewLike(reviewId: Long?, likedByMe: Boolean) {
        val id = reviewId ?: return showInteractionError()
        mutateReview(onSuccess = {
            updateReview(id) { review ->
                review.copy(
                    likedByMe = !likedByMe,
                    likeCount = (review.likeCount + if (likedByMe) -1 else 1).coerceAtLeast(0),
                )
            }
        }) { actions -> actions.toggleLike(id, likedByMe, accessToken) }
    }

    fun createReply(reviewId: Long?, content: String, onSuccess: () -> Unit = {}) {
        val id = reviewId ?: return showInteractionError()
        mutateReview(onSuccess = {
            updateReview(id) { review -> review.copy(replyCount = review.replyCount + 1) }
            onSuccess()
        }) { actions -> actions.createReply(id, content, accessToken) }
    }

    private fun loadFirstPage() {
        initialLoadJob?.cancel()
        loadMoreJob?.cancel()
        initialLoadJob = viewModelScope.launch {
            val previousState = _uiState.value
            withDelayedApiLoading(
                onLoadingChanged = { loading ->
                    if (loading) {
                        _uiState.value = FeedUiState.Loading
                    } else if (_uiState.value == FeedUiState.Loading) {
                        _uiState.value = previousState
                    }
                },
            ) {
                _uiState.value = try {
                    val page = repository.reviewFeed(FIRST_PAGE, accessToken)
                    if (page.reviews.isEmpty()) {
                        FeedUiState.Empty
                    } else {
                        FeedUiState.Content(
                            reviews = page.reviews.map { it.toUiModel() },
                            totalCount = page.totalCount,
                            nextPage = page.nextPage,
                        )
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    FeedUiState.Failure(error.toAppError())
                }
            }
        }
    }

    private fun mutateReview(
        onSuccess: () -> Unit,
        action: suspend (FeedReviewActions) -> Unit,
    ) {
        if (mutationJob?.isActive == true) return
        mutationJob = viewModelScope.launch {
            runCatching { action(reviewActions) }
                .onSuccess { onSuccess() }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    showInteractionError()
                }
        }
    }

    private fun updateReview(
        reviewId: Long,
        transform: (QuoteCardUiModel) -> QuoteCardUiModel,
    ) {
        updateContent { content ->
            content.copy(
                reviews = content.reviews.map { review ->
                    if (review.reviewId == reviewId) transform(review) else review
                },
            )
        }
    }

    private fun showInteractionError() {
        updateContent { it.copy(requestError = "요청을 처리하지 못했어요. 다시 시도해 주세요.") }
    }

    private fun updateContent(transform: (FeedUiState.Content) -> FeedUiState.Content) {
        (_uiState.value as? FeedUiState.Content)?.let { _uiState.value = transform(it) }
    }

    private fun QuoteCard.toUiModel() = QuoteCardUiModel(
        reviewId = reviewId,
        noteId = noteId,
        bookId = bookId,
        isbn13 = isbn13,
        bookTitle = bookTitle,
        coverId = coverId,
        authorName = authorLabel,
        timeLabel = TimeLabels.relative(createdAt, clock.now()),
        quoteText = quoteText,
        quote = quote,
        likeCount = likeCount,
        likedByMe = likedByMe,
        replyCount = replyCount,
        replyLabel = HomeLabels.quoteReply(replyCount),
        isSpoiler = isSpoiler,
    )

    private companion object {
        const val FIRST_PAGE = 1
    }
}
