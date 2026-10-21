package com.chaekchaek.app.ui.feed

import com.chaekchaek.app.presentation.common.ErrorRecoveryState
import com.chaekchaek.app.presentation.common.ErrorOperation
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.domain.feed.FeedReviewActions
import com.chaekchaek.app.domain.feed.FeedViewer
import com.chaekchaek.app.domain.feed.QuoteCard
import com.chaekchaek.app.domain.feed.ReviewFeedRepository
import com.chaekchaek.app.presentation.common.TimeLabels
import com.chaekchaek.app.presentation.common.withDelayedApiLoading
import com.chaekchaek.app.presentation.home.HomeLabels
import com.chaekchaek.app.presentation.home.QuoteCardUiModel
import kotlinx.coroutines.awaitCancellation
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
    reviewActions: FeedReviewActions,
    readGuestToken: () -> String? = { null },
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    initialAccessToken: String? = null,
    deferInitialLoad: Boolean = false,
) : ViewModel() {
    val errors = ErrorRecoveryState()
    private var reviewActions = reviewActions
    private var readGuestToken = readGuestToken
    private var accessToken: String? = initialAccessToken
    private var awaitingInitialAuthentication = deferInitialLoad
    private var authenticationRevision = 0L
    private var initialLoadJob: Job? = null
    private var loadMoreJob: Job? = null
    private var mutationJob: Job? = null
    private val _uiState = MutableStateFlow<FeedUiState>(
        FeedUiState.Content(emptyList(), totalCount = 0, nextPage = null),
    )
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        if (deferInitialLoad) {
            initialLoadJob = viewModelScope.launch {
                withDelayedApiLoading(onLoadingChanged = { loading ->
                    if (loading) _uiState.value = FeedUiState.Loading
                }) { awaitCancellation() }
            }
        } else loadFirstPage()
    }

    fun retry() {
        loadFirstPage()
    }

    fun attachActions(
        reviewActions: FeedReviewActions,
        readGuestToken: () -> String?,
    ) {
        this.reviewActions = reviewActions
        this.readGuestToken = readGuestToken
    }

    fun authenticate(accessToken: String?) {
        if (awaitingInitialAuthentication) {
            awaitingInitialAuthentication = false
            this.accessToken = accessToken
            loadFirstPage()
            return
        }
        if (this.accessToken == accessToken) return
        val renewingMemberSession = this.accessToken != null && accessToken != null
        authenticationRevision += 1
        mutationJob?.cancel()
        this.accessToken = accessToken
        if (!renewingMemberSession) savedStateHandle[FeedSavedStateKeys.LoadedPage] = FIRST_PAGE
        loadFirstPage(retainContent = renewingMemberSession)
    }

    fun loadMore() {
        val current = _uiState.value as? FeedUiState.Content ?: return
        val page = current.nextPage ?: return
        if (current.loadingMore || initialLoadJob?.isActive == true || loadMoreJob?.isActive == true) return
        _uiState.value = current.copy(loadingMore = true, requestError = null)
        val authenticationRevision = authenticationRevision
        val viewer = currentViewer()
        loadMoreJob = viewModelScope.launch {
            try {
                val next = repository.reviewFeed(page, viewer)
                if (authenticationRevision != this@FeedViewModel.authenticationRevision) return@launch
                val latest = _uiState.value as? FeedUiState.Content ?: return@launch
                if (latest.nextPage != page) return@launch
                savedStateHandle[FeedSavedStateKeys.LoadedPage] = page
                _uiState.value = latest.copy(
                    reviews = (latest.reviews + next.reviews.map { it.toUiModel() })
                        .distinctBy { it.noteId.value },
                    totalCount = next.totalCount,
                    nextPage = next.nextPage,
                    loadingMore = false,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (authenticationRevision != this@FeedViewModel.authenticationRevision) return@launch
                errors.report(error, ErrorOperation.FeedLoad) { freshToken ->
                    if (freshToken != null) this@FeedViewModel.accessToken = freshToken
                    loadMore()
                }
                updateContent {
                    it.copy(
                        loadingMore = false,
                        requestError = FeedRequestError.LoadMore,
                    )
                }
            }
        }
    }

    fun clearRequestError() {
        updateContent { it.copy(requestError = null) }
    }

    fun retryLoadMore() {
        val content = _uiState.value as? FeedUiState.Content ?: return
        if (content.requestError != FeedRequestError.LoadMore) return
        loadMore()
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
        }) { actions, accessToken -> actions.toggleLike(id, likedByMe, accessToken) }
    }

    fun createReply(reviewId: Long?, content: String, onSuccess: () -> Unit = {}) {
        val id = reviewId ?: return showInteractionError()
        mutateReview(onSuccess = {
            updateReview(id) { review -> review.copy(replyCount = review.replyCount + 1) }
            onSuccess()
        }) { actions, accessToken -> actions.createReply(id, content, accessToken) }
    }

    private fun loadFirstPage(retainContent: Boolean = false) {
        if (awaitingInitialAuthentication) return
        initialLoadJob?.cancel()
        loadMoreJob?.cancel()
        if (retainContent) updateContent { it.copy(loadingMore = false) }
        val authenticationRevision = authenticationRevision
        val viewer = currentViewer()
        initialLoadJob = viewModelScope.launch {
            val previousState = _uiState.value
            withDelayedApiLoading(
                onLoadingChanged = { loading ->
                    if (authenticationRevision != this@FeedViewModel.authenticationRevision) return@withDelayedApiLoading
                    if (loading && previousState !is FeedUiState.Content) {
                        _uiState.value = FeedUiState.Loading
                    } else if (_uiState.value == FeedUiState.Loading) {
                        _uiState.value = previousState
                    }
                },
            ) {
                _uiState.value = try {
                    val restoreThroughPage = savedStateHandle.get<Int>(FeedSavedStateKeys.LoadedPage) ?: FIRST_PAGE
                    var page = repository.reviewFeed(FIRST_PAGE, viewer)
                    var loadedPage = FIRST_PAGE
                    while (true) {
                        val nextPage = page.nextPage?.takeIf { it > loadedPage && it <= restoreThroughPage }
                            ?: break
                        val next = repository.reviewFeed(nextPage, viewer)
                        page = next.copy(reviews = (page.reviews + next.reviews).distinctBy { it.noteId.value })
                        loadedPage = nextPage
                    }
                    if (authenticationRevision != this@FeedViewModel.authenticationRevision) return@withDelayedApiLoading
                    savedStateHandle[FeedSavedStateKeys.LoadedPage] = loadedPage
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
                    if (authenticationRevision != this@FeedViewModel.authenticationRevision) {
                        return@withDelayedApiLoading
                    }
                    errors.report(error, ErrorOperation.FeedLoad) { freshToken ->
                        if (freshToken != null) this@FeedViewModel.accessToken = freshToken
                        loadFirstPage(retainContent = true)
                    }
                    previousState.takeUnless { it == FeedUiState.Loading } ?: FeedUiState.Content(emptyList(), 0, null)
                }
            }
        }
    }

    private fun mutateReview(
        onSuccess: () -> Unit,
        action: suspend (FeedReviewActions, accessToken: String?) -> Unit,
    ) {
        if (awaitingInitialAuthentication || mutationJob?.isActive == true) return
        val authenticationRevision = authenticationRevision
        val accessToken = accessToken
        mutationJob = viewModelScope.launch {
            runCatching { action(reviewActions, accessToken) }
                .onSuccess {
                    if (authenticationRevision == this@FeedViewModel.authenticationRevision) onSuccess()
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    if (authenticationRevision == this@FeedViewModel.authenticationRevision) {
                        errors.report(error, ErrorOperation.Request) { freshToken ->
                            if (freshToken != null) this@FeedViewModel.accessToken = freshToken
                            mutateReview(onSuccess, action)
                        }
                        showInteractionError()
                    }
                }
        }
    }

    private fun currentViewer(): FeedViewer = accessToken
        ?.let(FeedViewer::Member)
        ?: readGuestToken()?.let(FeedViewer::Guest)
        ?: FeedViewer.Anonymous

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
        if (errors.notice.value == null) errors.confirm("이 감상을 찾을 수 없어요.", "요청을 처리하지 못했어요")
        updateContent { it.copy(requestError = FeedRequestError.Interaction) }
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

internal object FeedSavedStateKeys {
    const val LoadedPage = "feed.loaded_page"
}
