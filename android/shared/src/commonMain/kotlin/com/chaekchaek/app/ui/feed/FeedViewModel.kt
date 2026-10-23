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
import com.chaekchaek.app.presentation.common.RefreshUiState
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
import kotlin.time.TimeSource
import com.chaekchaek.app.analytics.AnalyticsTracker
import com.chaekchaek.app.analytics.analyticsErrorCategory

class FeedViewModel(
    private val repository: ReviewFeedRepository,
    private val clock: Clock,
    reviewActions: FeedReviewActions,
    readGuestToken: () -> String? = { null },
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    initialAccessToken: String? = null,
    deferInitialLoad: Boolean = false,
    private val analytics: AnalyticsTracker = AnalyticsTracker.None,
) : ViewModel() {
    val errors = ErrorRecoveryState(analytics.errors)
    private var reviewActions = reviewActions
    private var readGuestToken = readGuestToken
    private var accessToken: String? = initialAccessToken
    private var awaitingInitialAuthentication = deferInitialLoad
    private var authenticationRevision = 0L
    private var loadRevision = 0L
    private val _refreshState = MutableStateFlow(RefreshUiState())
    val refreshState: StateFlow<RefreshUiState> = _refreshState.asStateFlow()
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
        loadFirstPage(trigger = "retry")
    }

    fun refresh() {
        if (awaitingInitialAuthentication || initialLoadJob?.isActive == true || mutationJob?.isActive == true) return
        loadFirstPage(retainContent = true, refreshing = true, trigger = "refresh")
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
        if (current.loadingMore || current.loadMoreFailed || current.requestError == FeedRequestError.LoadMore ||
            initialLoadJob?.isActive == true || loadMoreJob?.isActive == true) return
        _uiState.value = current.copy(loadingMore = true, requestError = null)
        val authenticationRevision = authenticationRevision
        val revision = loadRevision
        val viewer = currentViewer()
        val started = TimeSource.Monotonic.markNow()
        val loadId = analytics.newId("load")
        loadMoreJob = viewModelScope.launch {
            try {
                val next = withDelayedApiLoading(onLoadingChanged = { loading ->
                    if (revision == loadRevision) updateContent { it.copy(showLoadingMore = loading) }
                }) { repository.reviewFeed(page, viewer) }
                if (revision != loadRevision || authenticationRevision != this@FeedViewModel.authenticationRevision) return@launch
                val latest = _uiState.value as? FeedUiState.Content ?: return@launch
                if (latest.nextPage != page) return@launch
                recordLoad(loadId, "pagination", "success", started.elapsedNow().inWholeMilliseconds, next.reviews.size, page)
                savedStateHandle[FeedSavedStateKeys.LoadedPage] = page
                _uiState.value = latest.copy(
                    reviews = (latest.reviews + next.reviews.map { it.toUiModel() })
                        .distinctBy { it.noteId.value },
                    totalCount = next.totalCount,
                    nextPage = next.nextPage,
                    loadingMore = false,
                    showLoadingMore = false,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (revision != loadRevision || authenticationRevision != this@FeedViewModel.authenticationRevision) return@launch
                recordLoad(loadId, "pagination", "failure", started.elapsedNow().inWholeMilliseconds, page = page, error = error)
                errors.report(error, ErrorOperation.FeedLoad) { freshToken ->
                    if (freshToken != null) this@FeedViewModel.accessToken = freshToken
                    retryLoadMore()
                }
                updateContent {
                    it.copy(
                        loadingMore = false,
                        showLoadingMore = false,
                        requestError = FeedRequestError.LoadMore,
                        loadMoreFailed = true,
                    )
                }
            }
        }
    }

    fun clearRequestError() {
        updateContent { if (it.requestError == FeedRequestError.Interaction) it.copy(requestError = null) else it }
    }

    fun retryLoadMore() {
        val content = _uiState.value as? FeedUiState.Content ?: return
        if (!content.loadMoreFailed && content.requestError != FeedRequestError.LoadMore) return
        updateContent { it.copy(requestError = null, loadMoreFailed = false) }
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

    private fun recordLoad(loadId: String, trigger: String, outcome: String, duration: Long, count: Int = 0, page: Int = 1, error: Throwable? = null) {
        analytics.log("cc_load_result", strings = buildMap {
            put("resource", "feed")
            put("load_id", loadId)
            put("trigger", trigger)
            put("outcome", outcome)
            error?.let { put("error_category", analyticsErrorCategory(it)) }
        }, longs = mapOf("duration_ms" to duration, "result_count" to count.toLong(), "page" to page.toLong()), includeActionContext = false)
    }

    private fun loadFirstPage(retainContent: Boolean = false, refreshing: Boolean = false, trigger: String = "initial") {
        if (awaitingInitialAuthentication) return
        val revision = ++loadRevision
        initialLoadJob?.cancel()
        loadMoreJob?.cancel()
        _refreshState.value = RefreshUiState(isRefreshing = refreshing)
        if (retainContent) updateContent { it.copy(loadingMore = false, showLoadingMore = false) }
        val authenticationRevision = authenticationRevision
        val viewer = currentViewer()
        val started = TimeSource.Monotonic.markNow()
        val loadId = analytics.newId("load")
        initialLoadJob = viewModelScope.launch {
            val previousState = _uiState.value
            withDelayedApiLoading(
                onLoadingChanged = { loading ->
                    if (revision != loadRevision || authenticationRevision != this@FeedViewModel.authenticationRevision) return@withDelayedApiLoading
                    if (!refreshing && loading && (previousState !is FeedUiState.Content || previousState.reviews.isEmpty())) {
                        _uiState.value = FeedUiState.Loading
                    } else if (!refreshing && _uiState.value == FeedUiState.Loading) {
                        _uiState.value = previousState
                    }
                },
            ) {
                val result = try {
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
                    if (revision != loadRevision || authenticationRevision != this@FeedViewModel.authenticationRevision) return@withDelayedApiLoading
                    recordLoad(loadId, trigger, if (page.reviews.isEmpty()) "empty" else "success", started.elapsedNow().inWholeMilliseconds, page.reviews.size)
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
                    if (revision != loadRevision || authenticationRevision != this@FeedViewModel.authenticationRevision) {
                        return@withDelayedApiLoading
                    }
                    recordLoad(loadId, trigger, "failure", started.elapsedNow().inWholeMilliseconds, error = error)
                    errors.report(error, ErrorOperation.FeedLoad) { freshToken ->
                        if (freshToken != null) this@FeedViewModel.accessToken = freshToken
                        loadFirstPage(retainContent = true, refreshing = refreshing, trigger = "retry")
                    }
                    previousState.takeUnless { it == FeedUiState.Loading } ?: FeedUiState.Content(emptyList(), 0, null)
                }
                if (revision == loadRevision) _uiState.value = result
            }
            if (revision == loadRevision) {
                _refreshState.value = _refreshState.value.copy(isRefreshing = false)
            }
        }
    }

    private fun mutateReview(
        onSuccess: () -> Unit,
        action: suspend (FeedReviewActions, accessToken: String?) -> Unit,
    ) {
        if (awaitingInitialAuthentication || mutationJob?.isActive == true) return
        if (_refreshState.value.isRefreshing) {
            // 사용자가 확정한 쓰기는 바로 실행하고, 늦은 갱신 응답이 결과를 덮지 못하게 한다.
            loadRevision += 1
            initialLoadJob?.cancel()
            _refreshState.value = RefreshUiState()
        }
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
        authorProfile = authorProfile,
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
