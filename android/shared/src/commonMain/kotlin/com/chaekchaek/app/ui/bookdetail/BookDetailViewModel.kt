package com.chaekchaek.app.ui.bookdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.analytics.AnalyticsAction
import com.chaekchaek.app.analytics.AnalyticsTracker
import com.chaekchaek.app.analytics.analyticsBookKey
import com.chaekchaek.app.analytics.analyticsErrorCategory
import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.data.remote.BookDetailRemoteRepository
import com.chaekchaek.app.data.remote.BookReview
import com.chaekchaek.app.data.remote.LibraryRecord
import com.chaekchaek.app.data.remote.LibraryRemoteRepository
import com.chaekchaek.app.data.remote.MobileAuthRemoteRepository
import com.chaekchaek.app.data.remote.RatingComparison
import com.chaekchaek.app.data.remote.ReviewCreateRequest
import com.chaekchaek.app.data.remote.ReviewScope
import com.chaekchaek.app.data.remote.ReviewSort
import com.chaekchaek.app.data.remote.ReviewPage
import com.chaekchaek.app.data.remote.WriteCredential
import com.chaekchaek.app.domain.rating.Rating
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlin.coroutines.cancellation.CancellationException

class BookDetailViewModel(
    private val repository: BookDetailRemoteRepository,
    private val libraryRepository: LibraryRemoteRepository,
    authPlatform: AuthPlatformCallbacks,
    private val authRepository: MobileAuthRemoteRepository = MobileAuthRemoteRepository(),
    private val analytics: AnalyticsTracker = AnalyticsTracker.None,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val restoredReviewScope = ReviewScope.entries
        .firstOrNull { it.name == savedStateHandle[BookDetailSavedStateKeys.ReviewScope] }
        ?: ReviewScope.ALL
    private val restoredReviewSort = ReviewSort.entries
        .firstOrNull { it.name == savedStateHandle[BookDetailSavedStateKeys.ReviewSort] }
        ?: ReviewSort.LATEST
    private val _uiState = MutableStateFlow(
        BookDetailUiState(reviewScope = restoredReviewScope, reviewSort = restoredReviewSort),
    )
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    private var authPlatform = authPlatform
    private var accessToken: String? = null
    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null
    private var repliesJob: Job? = null
    private var mutationJob: Job? = null
    private var ratingComparisonJob: Job? = null
    private var reviewPageGeneration = 0
    private var requestGeneration = 0
    private var detailReadyLogged = false
    private var pendingAnalyticsAction: AnalyticsAction? = null
    private var externalLibrarySaveRevision = 0
    private var externallySavedRecord: LibraryRecord? = null

    fun attachAuthPlatform(authPlatform: AuthPlatformCallbacks) {
        this.authPlatform = authPlatform
    }

    fun open(book: BookDetailArgs, accessToken: String?) {
        if (_uiState.value.book == book) {
            syncAuthentication(accessToken)
            return
        }
        invalidateRequests()
        this.accessToken = accessToken
        val reviewScope = restoredReviewScope.takeIf { it != ReviewScope.MINE || accessToken != null }
            ?: ReviewScope.ALL.also {
                savedStateHandle[BookDetailSavedStateKeys.ReviewScope] = it.name
                savedStateHandle[BookDetailSavedStateKeys.LoadedPage] = 1
            }
        val restoring = savedStateHandle.get<Boolean>(BookDetailSavedStateKeys.Opened) == true
        savedStateHandle[BookDetailSavedStateKeys.Opened] = true
        _uiState.value = BookDetailUiState(
            book = book,
            reviewScope = reviewScope,
            reviewSort = restoredReviewSort,
            signedIn = accessToken != null,
            isRestoring = restoring,
            guestNickname = accessToken?.let { null } ?: authPlatform.readGuest()?.nickname,
        )
        detailReadyLogged = false
        analytics.ensureFlow("book_detail")
        reload()
    }

    fun authenticate(accessToken: String): BookDetailAuthenticatedAction? {
        return syncAuthentication(accessToken)
    }

    fun signOut() {
        syncAuthentication(null)
    }

    fun syncAuthentication(accessToken: String?): BookDetailAuthenticatedAction? {
        if (this.accessToken == accessToken) return null
        invalidateRequests()
        val previousAccessToken = this.accessToken
        this.accessToken = accessToken
        val pending = _uiState.value.pendingAction.takeIf { accessToken != null }
        val renewingMemberSession = previousAccessToken != null && accessToken != null
        if (!renewingMemberSession) savedStateHandle[BookDetailSavedStateKeys.LoadedPage] = 1
        val reviewScope = _uiState.value.reviewScope.takeIf {
            it != ReviewScope.MINE || accessToken != null
        } ?: ReviewScope.ALL.also { savedStateHandle[BookDetailSavedStateKeys.ReviewScope] = it.name }
        _uiState.value = _uiState.value.copy(
            signedIn = accessToken != null,
            reviewScope = reviewScope,
            pendingAction = null,
            guestNickname = accessToken?.let { null } ?: authPlatform.readGuest()?.nickname,
            ratingComparison = emptyList(),
            isLoading = false,
            isLoadingMore = false,
            isSubmitting = false,
            isSavingLibrary = false,
            libraryMessage = null,
            reviewSubmissionState = ReviewSubmissionState.Idle,
        )
        reload()
        return pending
    }

    fun requestAuthentication(action: BookDetailAuthenticatedAction): Boolean {
        if (accessToken != null || !action.requiresMember) return true
        if (_uiState.value.pendingAction == null) {
            pendingAnalyticsAction = analytics.startAction(
                action.analyticsName,
                strings = buildMap {
                    val book = _uiState.value.displayBook
                    analyticsBookKey(book?.isbn13, book?.id)?.let { put("book_key", it) }
                },
            )
        }
        _uiState.value = _uiState.value.copy(pendingAction = action)
        analytics.log(
            name = "cc_auth_prompt",
            strings = mapOf("trigger_action" to action.analyticsName, "surface" to "sheet"),
        )
        return false
    }

    fun dismissAuthentication() {
        _uiState.value.pendingAction?.let { pending ->
            analytics.log("cc_auth_dismiss", strings = mapOf("trigger_action" to pending.analyticsName))
        }
        pendingAnalyticsAction?.let { analytics.finishAction(it, "cancelled") }
        pendingAnalyticsAction = null
        _uiState.value = _uiState.value.copy(pendingAction = null)
    }

    fun retry() = reload()

    fun clearRequestError() {
        _uiState.value = _uiState.value.copy(requestError = null)
    }

    fun changeReviewScope(scope: ReviewScope) {
        if (scope == ReviewScope.MINE &&
            !requestAuthentication(BookDetailAuthenticatedAction.OpenMineFeed)
        ) return
        if (_uiState.value.reviewScope == scope) return
        _uiState.value = _uiState.value.copy(reviewScope = scope)
        savedStateHandle[BookDetailSavedStateKeys.ReviewScope] = scope.name
        savedStateHandle[BookDetailSavedStateKeys.LoadedPage] = 1
        reloadReviews()
    }

    fun changeReviewSort(sort: ReviewSort) {
        if (_uiState.value.reviewSort == sort) return
        _uiState.value = _uiState.value.copy(reviewSort = sort)
        savedStateHandle[BookDetailSavedStateKeys.ReviewSort] = sort.name
        savedStateHandle[BookDetailSavedStateKeys.LoadedPage] = 1
        reloadReviews()
    }

    fun loadMoreReviews() = requestNextReviewPage(retryAfterFailure = false)

    fun retryReviews() {
        when (_uiState.value.reviewPageLoadState) {
            ReviewPageLoadState.Failed -> requestNextReviewPage(retryAfterFailure = true)
            ReviewPageLoadState.RefreshFailed -> reload(retryingReviews = true)
            else -> Unit
        }
    }

    private fun requestNextReviewPage(retryAfterFailure: Boolean) {
        val state = _uiState.value
        val bookId = state.detail?.bookId ?: return
        val page = state.nextReviewPage ?: return
        val expectedState = if (retryAfterFailure) ReviewPageLoadState.Failed else ReviewPageLoadState.Idle
        if (state.reviewPageLoadState != expectedState || loadMoreJob?.isActive == true) return
        val generation = reviewPageGeneration
        val requestedScope = state.reviewScope
        val requestedSort = state.reviewSort
        _uiState.value = state.copy(
            reviewPageLoadState = if (retryAfterFailure) ReviewPageLoadState.Retrying else ReviewPageLoadState.Loading,
        )
        loadMoreJob = viewModelScope.launch {
            runCatching {
                withDelayedLoading(
                    onLoadingChanged = { loading ->
                        if (generation == reviewPageGeneration) {
                            _uiState.value = _uiState.value.copy(isLoadingMore = loading)
                        }
                    },
                ) {
                    repository.reviews(bookId, requestedScope, requestedSort, readCredential(), page)
                }
            }.onSuccess { result ->
                if (generation != reviewPageGeneration) return@onSuccess
                val current = _uiState.value
                if (current.reviewScope != requestedScope || current.reviewSort != requestedSort) return@onSuccess
                savedStateHandle[BookDetailSavedStateKeys.LoadedPage] = page
                _uiState.value = current.copy(
                    reviews = (current.reviews + result.items).distinctBy(BookReview::reviewId),
                    reviewCount = result.totalCount,
                    nextReviewPage = result.nextPage,
                    reviewPageLoadState = ReviewPageLoadState.Idle,
                )
            }.onFailure { error ->
                if (error is CancellationException) throw error
                if (generation == reviewPageGeneration) {
                    _uiState.value = _uiState.value.copy(reviewPageLoadState = ReviewPageLoadState.Failed)
                }
            }
        }
    }

    private fun resetReviewPagination() {
        reviewPageGeneration += 1
        loadMoreJob?.cancel()
        _uiState.value = _uiState.value.copy(
            nextReviewPage = null,
            isLoading = false,
            isLoadingMore = false,
            reviewPageLoadState = ReviewPageLoadState.Idle,
        )
    }

    fun addToLibrary(onSuccess: (LibraryRecord) -> Unit = {}) {
        if (mutationJob?.isActive == true || _uiState.value.detail?.myRecord != null) return
        _uiState.value = _uiState.value.copy(isSavingLibrary = true)
        mutate(
            actionName = "library_add",
            onFinished = { _uiState.value = _uiState.value.copy(isSavingLibrary = false) },
            onSuccess = { record: LibraryRecord ->
                updateMyRecord(record)
                _uiState.value = _uiState.value.copy(libraryMessage = "내 서재에 담았어요")
                onSuccess(record)
            },
            reloadAfterSuccess = false,
        ) {
            val state = _uiState.value
            state.detail?.myRecord?.let { return@mutate it }
            requireNotNull(state.detail)
            val book = requireNotNull(state.displayBook)
            repository.addToLibrary(book.isbn13, book.totalPages.takeIf { it > 0 }, requireToken())
        }
    }

    fun acceptSavedLibraryRecord(isbn13: String, record: LibraryRecord) {
        if (accessToken == null || _uiState.value.displayBook?.isbn13 != isbn13) return
        if (_uiState.value.detail?.myRecord != null) return
        externalLibrarySaveRevision += 1
        externallySavedRecord = record
        updateMyRecord(record)
    }

    fun removeFromLibrary(savedBookId: Long, onSuccess: () -> Unit = {}) = mutate(
        actionName = "library_remove",
        onSuccess = { _: Unit ->
            updateMyRecord(null)
            _uiState.value = _uiState.value.copy(libraryMessage = "서재에서 삭제했어요")
            onSuccess()
        },
        reloadAfterSuccess = false,
    ) {
        libraryRepository.bulkDelete(listOf(savedBookId), requireToken())
    }

    fun clearLibraryMessage() {
        _uiState.value = _uiState.value.copy(libraryMessage = null)
    }

    fun updateStatus(
        status: ReadingStatus,
        onLibraryAdded: () -> Unit = {},
        onSuccess: () -> Unit = {},
    ) = mutateLibraryRecord("reading_status_change", onSuccess, onLibraryAdded) { bookId, token ->
        repository.updateReadingStatus(bookId, status.apiValue, token)
    }

    fun savePage(
        page: Int,
        onLibraryAdded: () -> Unit = {},
        onSuccess: () -> Unit = {},
    ) = mutateLibraryRecord("reading_page_save", onSuccess, onLibraryAdded) { bookId, token ->
        val totalPages = _uiState.value.detail?.totalPages ?: _uiState.value.displayBook?.totalPages
        repository.updateCurrentPage(bookId, page, totalPages, token)
    }

    fun saveRating(
        rating: Rating,
        onLibraryAdded: () -> Unit = {},
        onSuccess: () -> Unit = {},
    ) = mutateLibraryRecord(
        actionName = "rating_save",
        onSuccess = onSuccess,
        onLibraryAdded = onLibraryAdded,
        reloadAfterSuccess = true,
    ) { bookId, token ->
        repository.rate(bookId, rating.score.toDouble(), token)
    }

    fun loadRatingComparison(criterion: Rating) {
        val token = accessToken ?: return
        val isbn13 = _uiState.value.displayBook?.isbn13?.takeIf(String::isNotBlank) ?: return
        ratingComparisonJob?.cancel()
        ratingComparisonJob = viewModelScope.launch {
            runCatching {
                repository.ratingComparison(isbn13, criterion.score.toDouble(), token)
            }.onSuccess { comparison ->
                if (accessToken == token) {
                    _uiState.value = _uiState.value.copy(ratingComparison = comparison.toUiModels())
                }
            }.onFailure(::handleFailure)
        }
    }

    fun createReview(request: ReviewCreateRequest) {
        if (mutationJob?.isActive == true) return
        _uiState.value = _uiState.value.copy(
            reviewSubmissionState = ReviewSubmissionState.Submitting,
            requestError = null,
        )
        val state = _uiState.value
        val target = reviewCreateTarget(state.displayBook)
        val analyticsContext = mapOf(
            "request_route" to (target?.analyticsRoute ?: "unresolved"),
            "book_id_state" to if (state.displayBook?.resolvableBookId() != null) "present" else "missing",
            "detail_load_state" to when {
                state.detail != null -> "ready"
                loadJob?.isActive == true -> "pending"
                state.reviewPageLoadState.canRetry -> "failed"
                else -> "not_started"
            },
        )
        var failureStage = "target_resolution"
        mutate(
            actionName = "review_create",
            actionStrings = analyticsContext,
            actionResultStrings = analyticsContext,
            failureActionStrings = { mapOf("failure_stage" to failureStage) },
            failureActionLongs = { error ->
                (error as? ResponseException)?.response?.status?.value?.toLong()
                    ?.let { mapOf("http_status" to it) }
                    .orEmpty()
            },
            awaitLoadBeforeAction = false,
            closesComposer = true,
            preservesComposerOnFailure = true,
            onSuccess = { _: BookReview ->
                _uiState.value = _uiState.value.copy(reviewSubmissionState = ReviewSubmissionState.Succeeded)
            },
            onFailure = {
                _uiState.value = _uiState.value.copy(reviewSubmissionState = ReviewSubmissionState.Failed)
            },
        ) {
            val resolvedTarget = target ?: throw ReviewCreateTargetUnavailableException()
            publicWrite(
                retryUnauthorized = true,
                beforeAuth = { failureStage = "auth" },
                beforeRequest = { failureStage = "request" },
            ) {
                when (resolvedTarget) {
                    is ReviewCreateTarget.ByIsbn ->
                        repository.createReviewByIsbn(resolvedTarget.isbn13, request, it)
                    is ReviewCreateTarget.ByBookId ->
                        repository.createReview(resolvedTarget.bookId, request, it)
                }
            }
        }
    }

    fun openReviewComposer(onReady: () -> Unit) {
        if (_uiState.value.reviewSubmissionState == ReviewSubmissionState.Submitting) return
        _uiState.value = _uiState.value.copy(reviewSubmissionState = ReviewSubmissionState.Idle)
        authPlatform.readGuest()?.let {
            _uiState.value = _uiState.value.copy(guestNickname = it.nickname)
        }
        if (accessToken != null || readCredential() != null) {
            openComposer("review", "create")
            onReady()
            return
        }
        mutate(
            actionName = "guest_session_create",
            onSuccess = {
                openComposer("review", "create")
                onReady()
            },
            reloadAfterSuccess = false,
        ) {
            issueGuestCredential()
        }
    }

    fun openReplyComposer() = openComposer("reply", "create")

    fun openReviewEditor() = openComposer("review", "edit")

    fun openReplyEditor() = openComposer("reply", "edit")

    fun dismissComposer() = analytics.dismissComposer()

    fun updateReview(reviewId: Long, request: ReviewCreateRequest) = mutate(
        actionName = "review_update",
        onSuccess = { updated ->
            _uiState.value = _uiState.value.copy(
                reviews = _uiState.value.reviews.map { if (it.reviewId == reviewId) updated else it },
            )
        },
        reloadAfterSuccess = false,
        closesComposer = true,
    ) {
        publicWrite(retryUnauthorized = false) { repository.updateReview(reviewId, request, it) }
    }

    fun deleteReview(reviewId: Long) = mutate(
        actionName = "review_delete",
        onSuccess = {
            _uiState.value = _uiState.value.copy(
                reviews = _uiState.value.reviews.filterNot { it.reviewId == reviewId },
                reviewCount = (_uiState.value.reviewCount - 1).coerceAtLeast(0),
            )
        },
        reloadAfterSuccess = false,
    ) {
        publicWrite(retryUnauthorized = false) { repository.deleteReview(reviewId, it) }
    }

    fun likeReview(reviewId: Long, likedByMe: Boolean) = mutate(
        actionName = "review_like",
        actionStrings = mapOf("new_state" to if (likedByMe) "unliked" else "liked"),
    ) {
        publicWrite(retryUnauthorized = !likedByMe) { credential ->
            if (likedByMe) repository.unlikeReview(reviewId, credential)
            else repository.likeReview(reviewId, credential)
        }
    }

    fun createReply(reviewId: Long, content: String) = mutate(
        actionName = "reply_create",
        closesComposer = true,
    ) {
        publicWrite(retryUnauthorized = true) { repository.createReply(reviewId, content, it) }
    }

    fun updateReply(replyId: Long, content: String) = mutate(
        actionName = "reply_update",
        onSuccess = { updated ->
            _uiState.value = _uiState.value.copy(
                reviews = _uiState.value.reviews.map { review ->
                    review.copy(
                        recentReplies = review.recentReplies.map { if (it.replyId == replyId) updated else it },
                    )
                },
            )
        },
        reloadAfterSuccess = false,
        closesComposer = true,
    ) {
        publicWrite(retryUnauthorized = false) { repository.updateReply(replyId, content, it) }
    }

    fun deleteReply(replyId: Long) = mutate(
        actionName = "reply_delete",
        onSuccess = {
            _uiState.value = _uiState.value.copy(
                reviews = _uiState.value.reviews.map { review ->
                    if (review.recentReplies.none { it.replyId == replyId }) review else review.copy(
                        recentReplies = review.recentReplies.filterNot { it.replyId == replyId },
                        replyCount = (review.replyCount - 1).coerceAtLeast(0),
                    )
                },
            )
        },
        reloadAfterSuccess = false,
    ) {
        publicWrite(retryUnauthorized = false) { repository.deleteReply(replyId, it) }
    }

    fun loadReplies(reviewId: Long) {
        repliesJob?.cancel()
        repliesJob = viewModelScope.launch {
            runCatching {
                loadAllReplies { page -> repository.replies(reviewId, readCredential(), page) }
            }.onSuccess { replies ->
                _uiState.value = _uiState.value.copy(
                    reviews = _uiState.value.reviews.map { review ->
                        if (review.reviewId == reviewId) review.copy(recentReplies = replies) else review
                    },
                )
            }.onFailure(::handleFailure)
        }
    }

    fun likeReply(replyId: Long, likedByMe: Boolean) = mutate(
        actionName = "reply_like",
        actionStrings = mapOf("new_state" to if (likedByMe) "unliked" else "liked"),
    ) {
        publicWrite(retryUnauthorized = !likedByMe) { credential ->
            if (likedByMe) repository.unlikeReply(replyId, credential)
            else repository.likeReply(replyId, credential)
        }
    }

    private fun reload(retryingReviews: Boolean = false) {
        val book = _uiState.value.book ?: return
        resetReviewPagination()
        _uiState.value = _uiState.value.copy(
            reviewPageLoadState = if (retryingReviews) ReviewPageLoadState.RefreshRetrying else ReviewPageLoadState.Loading,
            requestError = null,
        )
        val reviewGeneration = reviewPageGeneration
        val generation = requestGeneration
        val credential = readCredential()
        val librarySaveRevision = externalLibrarySaveRevision
        val reviewScope = _uiState.value.reviewScope
        val reviewSort = _uiState.value.reviewSort
        val loadId = analytics.newId("load")
        val loadTrigger = if (detailReadyLogged) "refresh" else "initial"
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val started = kotlin.time.TimeSource.Monotonic.markNow()
            runCatching {
                withDelayedLoading(
                    onLoadingChanged = { loading ->
                        if (generation == requestGeneration && reviewGeneration == reviewPageGeneration) {
                            _uiState.value = _uiState.value.copy(isLoading = loading)
                        }
                    },
                ) {
                    val detail = book.isbn13.takeIf(String::isNotBlank)
                        ?.let { repository.detail(it, credential) }
                    val reviews = detail?.bookId?.let { bookId ->
                        restoreReviewPages(
                            bookId,
                            reviewScope,
                            reviewSort,
                            credential,
                        )
                    }
                    detail to reviews
                }
            }.onSuccess { (detail, reviews) ->
                if (generation != requestGeneration || reviewGeneration != reviewPageGeneration) return@onSuccess
                _uiState.value = _uiState.value.copy(
                    detail = if (librarySaveRevision != externalLibrarySaveRevision) {
                        detail?.copy(myRecord = _uiState.value.detail?.myRecord ?: externallySavedRecord)
                    } else detail,
                    reviews = reviews?.items.orEmpty(),
                    reviewCount = reviews?.totalCount ?: 0,
                    nextReviewPage = reviews?.nextPage,
                    isRestoring = false,
                    reviewPageLoadState = ReviewPageLoadState.Idle,
                )
                val bookKey = analyticsBookKey(detail?.isbn13 ?: book.isbn13, detail?.bookId?.toString() ?: book.id)
                analytics.log(
                    name = "cc_load_result",
                    strings = buildMap {
                        put("load_id", loadId)
                        put("resource", "book_detail")
                        put("trigger", loadTrigger)
                        bookKey?.let { put("book_key", it) }
                        put("outcome", "success")
                    },
                    longs = mapOf("duration_ms" to started.elapsedNow().inWholeMilliseconds),
                    includeActionContext = false,
                )
                if (!detailReadyLogged) analytics.log(
                    name = "cc_book_detail_ready",
                    strings = buildMap {
                        bookKey?.let { put("book_key", it) }
                        put("outcome", "success")
                        put("has_my_record", (detail?.myRecord != null).toString())
                        put(
                            "review_count_bucket",
                            when (reviews?.totalCount ?: 0) {
                                0 -> "0"
                                in 1..5 -> "1_5"
                                else -> "6_plus"
                            },
                        )
                    },
                    longs = mapOf("duration_ms" to started.elapsedNow().inWholeMilliseconds),
                    includeActionContext = false,
                )
                detailReadyLogged = true
            }.onFailure { error ->
                if (error is CancellationException) throw error
                if (generation == requestGeneration && reviewGeneration == reviewPageGeneration) {
                    _uiState.value = _uiState.value.copy(
                        reviewPageLoadState = ReviewPageLoadState.RefreshFailed,
                        isRestoring = false,
                    )
                    analytics.log(
                        name = "cc_load_result",
                        strings = mapOf(
                            "load_id" to loadId,
                            "resource" to "book_detail",
                            "trigger" to loadTrigger,
                            "outcome" to "failure",
                            "error_category" to analyticsErrorCategory(error),
                        ),
                        longs = mapOf("duration_ms" to started.elapsedNow().inWholeMilliseconds),
                        includeActionContext = false,
                    )
                }
            }
        }
    }

    private fun reloadReviews() {
        val bookId = _uiState.value.detail?.bookId ?: return
        resetReviewPagination()
        val generation = reviewPageGeneration
        val scope = _uiState.value.reviewScope
        val sort = _uiState.value.reviewSort
        _uiState.value = _uiState.value.copy(reviewPageLoadState = ReviewPageLoadState.Loading, requestError = null)
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            runCatching {
                withDelayedLoading(
                    onLoadingChanged = { loading ->
                        if (generation == reviewPageGeneration) {
                            _uiState.value = _uiState.value.copy(isLoading = loading)
                        }
                    },
                ) {
                    restoreReviewPages(
                        bookId,
                        scope,
                        sort,
                        readCredential(),
                    )
                }
            }.onSuccess { reviews ->
                if (generation != reviewPageGeneration) return@onSuccess
                _uiState.value = _uiState.value.copy(
                    reviews = reviews.items,
                    reviewCount = reviews.totalCount,
                    nextReviewPage = reviews.nextPage,
                    reviewPageLoadState = ReviewPageLoadState.Idle,
                )
            }.onFailure { error ->
                if (error is CancellationException) throw error
                if (generation == reviewPageGeneration) {
                    _uiState.value = _uiState.value.copy(
                        reviewPageLoadState = ReviewPageLoadState.RefreshFailed,
                        isRestoring = false,
                    )
                }
            }
        }
    }

    private suspend fun restoreReviewPages(
        bookId: Long,
        scope: ReviewScope,
        sort: ReviewSort,
        credential: WriteCredential?,
    ): ReviewPage {
        val restoreThroughPage = savedStateHandle.get<Int>(BookDetailSavedStateKeys.LoadedPage) ?: 1
        var page = repository.reviews(bookId, scope, sort, credential)
        var loadedPage = 1
        while (true) {
            val nextPage = page.nextPage?.takeIf { it > loadedPage && it <= restoreThroughPage } ?: break
            val next = repository.reviews(bookId, scope, sort, credential, nextPage)
            page = next.copy(items = (page.items + next.items).distinctBy(BookReview::reviewId))
            loadedPage = nextPage
        }
        savedStateHandle[BookDetailSavedStateKeys.LoadedPage] = loadedPage
        return page
    }

    private fun <T> mutate(
        actionName: String? = null,
        actionStrings: Map<String, String> = emptyMap(),
        actionResultStrings: Map<String, String> = emptyMap(),
        failureActionStrings: (Throwable) -> Map<String, String> = { emptyMap() },
        failureActionLongs: (Throwable) -> Map<String, Long> = { emptyMap() },
        onSuccess: (T) -> Unit = {},
        onFailure: (Throwable) -> Unit = {},
        reloadAfterSuccess: Boolean = true,
        awaitLoadBeforeAction: Boolean = true,
        closesComposer: Boolean = false,
        onFinished: () -> Unit = {},
        preservesComposerOnFailure: Boolean = false,
        action: suspend () -> T,
    ) {
        if (mutationJob?.isActive == true) return
        val generation = requestGeneration
        val book = _uiState.value.displayBook
        val actionHandle = actionName?.let { name ->
            pendingAnalyticsAction?.takeIf { it.action == name }?.also {
                pendingAnalyticsAction = null
            } ?: analytics.startAction(
                name,
                strings = buildMap {
                    analyticsBookKey(book?.isbn13, book?.id)?.let { put("book_key", it) }
                    putAll(actionStrings)
                },
            )
        }
        mutationJob = viewModelScope.launch {
            runCatching {
                withDelayedLoading(
                    onLoadingChanged = { loading ->
                        if (generation == requestGeneration) {
                            _uiState.value = _uiState.value.copy(isSubmitting = loading)
                        }
                    },
                ) {
                    if (awaitLoadBeforeAction) loadJob?.join()
                    action()
                }
            }.onSuccess { result ->
                if (generation != requestGeneration) return@onSuccess
                loadJob?.cancel()
                onSuccess(result)
                actionHandle?.let { analytics.finishAction(it, "success", strings = actionResultStrings) }
                if (closesComposer) analytics.completeComposer()
                if (reloadAfterSuccess) reload()
            }.onFailure { error ->
                actionHandle?.let {
                    analytics.finishAction(
                        it,
                        if (error is CancellationException) "cancelled" else "failure",
                        strings = if (error is CancellationException) emptyMap() else {
                            actionResultStrings +
                                mapOf("error_category" to analyticsErrorCategory(error)) +
                                failureActionStrings(error)
                        },
                        longs = if (error is CancellationException) emptyMap() else failureActionLongs(error),
                    )
                }
                if (closesComposer && !preservesComposerOnFailure) analytics.completeComposer()
                if (generation == requestGeneration && error !is CancellationException) onFailure(error)
                if (generation == requestGeneration || error is CancellationException) handleFailure(error)
            }
            if (generation == requestGeneration) onFinished()
        }
    }

    private fun openComposer(contentType: String, mode: String) {
        val book = _uiState.value.displayBook
        analytics.openComposer(
            contentType = contentType,
            mode = mode,
            bookKey = analyticsBookKey(book?.isbn13, book?.id),
        )
    }

    private fun mutateLibraryRecord(
        actionName: String,
        onSuccess: () -> Unit,
        onLibraryAdded: () -> Unit,
        reloadAfterSuccess: Boolean = false,
        action: suspend (bookId: Long, token: String) -> LibraryRecord,
    ) {
        val generation = requestGeneration
        mutate(
            actionName = actionName,
            onSuccess = { record: LibraryRecord ->
                updateMyRecord(record)
                onSuccess()
            },
            reloadAfterSuccess = reloadAfterSuccess,
        ) {
            val token = requireToken()
            action(
                bookIdForWrite(token, generation, onLibraryAdded, actionName),
                token,
            )
        }
    }

    private suspend fun bookIdForWrite(
        token: String,
        generation: Int,
        onLibraryAdded: () -> Unit,
        causedByAction: String,
    ): Long {
        val detail = requireNotNull(_uiState.value.detail)
        detail.myRecord?.bookId?.let { return it }
        if (detail.myRecord != null) return requireNotNull(detail.bookId)
        val book = requireNotNull(_uiState.value.displayBook)
        val record = repository.addToLibrary(
            book.isbn13,
            book.totalPages.takeIf { it > 0 },
            token,
        )
        if (generation != requestGeneration) throw CancellationException("인증 상태가 변경됨")
        updateMyRecord(record)
        onLibraryAdded()
        analytics.log(
            name = "cc_library_auto_add",
            strings = buildMap {
                analyticsBookKey(book.isbn13, book.id)?.let { put("book_key", it) }
                put("caused_by_action", causedByAction)
            },
        )
        return requireNotNull(record.bookId ?: _uiState.value.detail?.bookId)
    }

    private fun invalidateRequests() {
        requestGeneration += 1
        externallySavedRecord = null
        resetReviewPagination()
        loadJob?.cancel()
        loadMoreJob?.cancel()
        repliesJob?.cancel()
        mutationJob?.cancel()
        ratingComparisonJob?.cancel()
    }

    private fun updateMyRecord(record: LibraryRecord?) {
        val detail = _uiState.value.detail ?: return
        _uiState.value = _uiState.value.copy(detail = detail.copy(myRecord = record))
    }

    private fun requireToken(): String = requireNotNull(accessToken)

    private fun readCredential(): WriteCredential? =
        accessToken?.let { WriteCredential.Member(it) }
            ?: authPlatform.readGuest()?.token?.let { WriteCredential.Guest(it) }

    private suspend fun writeCredential(): WriteCredential = readCredential() ?: issueGuestCredential()

    private suspend fun issueGuestCredential(): WriteCredential.Guest {
        val guest = authRepository.issueGuest()
        authPlatform.writeGuest(guest)
        _uiState.value = _uiState.value.copy(guestNickname = guest.nickname)
        return WriteCredential.Guest(guest.token)
    }

    private suspend fun <T> publicWrite(
        retryUnauthorized: Boolean,
        beforeAuth: () -> Unit = {},
        beforeRequest: () -> Unit = {},
        request: suspend (WriteCredential) -> T,
    ): T {
        beforeAuth()
        val credential = writeCredential()
        return try {
            beforeRequest()
            request(credential)
        } catch (error: Throwable) {
            if (credential !is WriteCredential.Guest || !error.isUnauthorized()) throw error
            if (!retryUnauthorized) throw GuestOwnershipLostException()
            beforeAuth()
            val renewedGuest = issueGuestCredential()
            beforeRequest()
            request(renewedGuest)
        }
    }

    private fun handleFailure(error: Throwable) {
        if (error is CancellationException) throw error
        _uiState.value = _uiState.value.copy(
            isRestoring = false,
            requestError = if (error is GuestOwnershipLostException) {
                "이 기기에서는 더 이상 수정할 수 없습니다."
            } else {
                "요청을 처리하지 못했어요. 다시 시도해 주세요."
            },
        )
    }

}

internal object BookDetailSavedStateKeys {
    const val Opened = "book_detail.opened"
    const val LoadedPage = "book_detail.loaded_page"
    const val ReviewScope = "book_detail.review_scope"
    const val ReviewSort = "book_detail.review_sort"
}

private class GuestOwnershipLostException : RuntimeException()

private class ReviewCreateTargetUnavailableException : RuntimeException()

private sealed interface ReviewCreateTarget {
    val analyticsRoute: String

    data class ByIsbn(val isbn13: String) : ReviewCreateTarget {
        override val analyticsRoute = "by_isbn"
    }

    data class ByBookId(val bookId: Long) : ReviewCreateTarget {
        override val analyticsRoute = "by_book_id"
    }
}

private fun reviewCreateTarget(book: BookDetailArgs?): ReviewCreateTarget? =
    book?.isbn13?.takeIf(String::isNotBlank)?.let(ReviewCreateTarget::ByIsbn)
        ?: book?.resolvableBookId()?.let(ReviewCreateTarget::ByBookId)

private fun BookDetailArgs.resolvableBookId(): Long? =
    bookId ?: id.toLongOrNull()?.takeIf { isbn13.isBlank() }

internal val BookDetailAuthenticatedAction.analyticsName: String
    get() = when (this) {
        BookDetailAuthenticatedAction.AddToLibrary -> "library_add"
        BookDetailAuthenticatedAction.OpenPageInput -> "reading_page_save"
        BookDetailAuthenticatedAction.OpenRating -> "rating_save"
        BookDetailAuthenticatedAction.OpenReview -> "review_create"
        BookDetailAuthenticatedAction.OpenMineFeed -> "mine_feed"
        is BookDetailAuthenticatedAction.ChangeStatus -> "reading_status_change"
        is BookDetailAuthenticatedAction.SavePage -> "reading_page_save"
        is BookDetailAuthenticatedAction.LikeReview -> "review_like"
        is BookDetailAuthenticatedAction.CreateReply -> "reply_create"
        is BookDetailAuthenticatedAction.LikeReply -> "reply_like"
        is BookDetailAuthenticatedAction.EditReview -> "review_update"
        is BookDetailAuthenticatedAction.DeleteReview -> "review_delete"
        is BookDetailAuthenticatedAction.EditReply -> "reply_update"
        is BookDetailAuthenticatedAction.DeleteReply -> "reply_delete"
    }

private fun RatingComparison.toUiModels(): List<RatingComparisonBookUiModel> =
    listOfNotNull(lower, current, higher).map {
        RatingComparisonBookUiModel(
            bookId = it.bookId,
            title = it.title,
            rating = it.myRating,
            ratedAtLabel = it.ratingUpdatedAt.take(10).replace('-', '.'),
        )
    }

private fun Throwable.isUnauthorized(): Boolean =
    this is ResponseException && response.status == HttpStatusCode.Unauthorized
