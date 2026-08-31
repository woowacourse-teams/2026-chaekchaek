package com.chaekchaek.app.ui.bookdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val authPlatform: AuthPlatformCallbacks,
    private val authRepository: MobileAuthRemoteRepository = MobileAuthRemoteRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    private var accessToken: String? = null
    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null
    private var repliesJob: Job? = null
    private var mutationJob: Job? = null
    private var ratingComparisonJob: Job? = null
    private var requestGeneration = 0

    fun open(book: BookDetailArgs, accessToken: String?) {
        invalidateRequests()
        this.accessToken = accessToken
        _uiState.value = BookDetailUiState(
            book = book,
            signedIn = accessToken != null,
            guestNickname = accessToken?.let { null } ?: authPlatform.readGuest()?.nickname,
        )
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
        this.accessToken = accessToken
        val pending = _uiState.value.pendingAction.takeIf { accessToken != null }
        _uiState.value = _uiState.value.copy(
            signedIn = accessToken != null,
            pendingAction = null,
            guestNickname = accessToken?.let { null } ?: authPlatform.readGuest()?.nickname,
            ratingComparison = emptyList(),
            isLoading = false,
            isLoadingMore = false,
            isSubmitting = false,
        )
        reload()
        return pending
    }

    fun requestAuthentication(action: BookDetailAuthenticatedAction): Boolean {
        if (accessToken != null || !action.requiresMember) return true
        _uiState.value = _uiState.value.copy(pendingAction = action)
        return false
    }

    fun dismissAuthentication() {
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
        reloadReviews()
    }

    fun changeReviewSort(sort: ReviewSort) {
        if (_uiState.value.reviewSort == sort) return
        _uiState.value = _uiState.value.copy(reviewSort = sort)
        reloadReviews()
    }

    fun loadMoreReviews() {
        val state = _uiState.value
        val bookId = state.detail?.bookId ?: return
        val page = state.nextReviewPage ?: return
        if (loadMoreJob?.isActive == true) return
        val requestedScope = state.reviewScope
        val requestedSort = state.reviewSort
        loadMoreJob = viewModelScope.launch {
            runCatching {
                withDelayedLoading(
                    onLoadingChanged = { loading ->
                        _uiState.value = _uiState.value.copy(isLoadingMore = loading)
                    },
                ) {
                    repository.reviews(bookId, requestedScope, requestedSort, readCredential(), page)
                }
            }.onSuccess { result ->
                val current = _uiState.value
                if (current.reviewScope != requestedScope || current.reviewSort != requestedSort) return@onSuccess
                _uiState.value = current.copy(
                    reviews = (current.reviews + result.items).distinctBy(BookReview::reviewId),
                    reviewCount = result.totalCount,
                    nextReviewPage = result.nextPage,
                )
            }.onFailure(::handleFailure)
        }
    }

    fun toggleLibrary(savedBookId: Long?, onSuccess: () -> Unit = {}) = mutate(
        onSuccess = { record: LibraryRecord? ->
            updateMyRecord(record)
            onSuccess()
        },
        reloadAfterSuccess = false,
    ) {
        val state = _uiState.value
        if (savedBookId == null) {
            state.detail?.myRecord?.let { return@mutate it }
            requireNotNull(state.detail)
            val book = requireNotNull(state.displayBook)
            repository.addToLibrary(book.isbn13, book.totalPages.takeIf { it > 0 }, requireToken())
        } else {
            libraryRepository.bulkDelete(listOf(savedBookId), requireToken())
            null
        }
    }

    fun updateStatus(
        status: ReadingStatus,
        onLibraryAdded: () -> Unit = {},
        onSuccess: () -> Unit = {},
    ) = mutateLibraryRecord(onSuccess, onLibraryAdded) { bookId, token ->
        repository.updateReadingStatus(bookId, status.apiValue, token)
    }

    fun savePage(
        page: Int,
        onLibraryAdded: () -> Unit = {},
        onSuccess: () -> Unit = {},
    ) = mutateLibraryRecord(onSuccess, onLibraryAdded) { bookId, token ->
        val totalPages = _uiState.value.detail?.totalPages ?: _uiState.value.displayBook?.totalPages
        repository.updateCurrentPage(bookId, page, totalPages, token)
    }

    fun saveRating(
        rating: Rating,
        onLibraryAdded: () -> Unit = {},
        onSuccess: () -> Unit = {},
    ) = mutateLibraryRecord(
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
        _uiState.value = _uiState.value.copy(ratingComparison = emptyList())
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

    fun createReview(request: ReviewCreateRequest) = mutate {
        publicWrite(retryUnauthorized = true) { repository.createReview(bookIdForPublicWrite(), request, it) }
    }

    fun openReviewComposer(onReady: () -> Unit) {
        authPlatform.readGuest()?.let {
            _uiState.value = _uiState.value.copy(guestNickname = it.nickname)
        }
        if (accessToken != null || readCredential() != null) {
            onReady()
            return
        }
        mutate(onSuccess = { onReady() }, reloadAfterSuccess = false) { issueGuestCredential() }
    }

    fun updateReview(reviewId: Long, request: ReviewCreateRequest) = mutate(
        onSuccess = { updated ->
            _uiState.value = _uiState.value.copy(
                reviews = _uiState.value.reviews.map { if (it.reviewId == reviewId) updated else it },
            )
        },
        reloadAfterSuccess = false,
    ) {
        publicWrite(retryUnauthorized = false) { repository.updateReview(reviewId, request, it) }
    }

    fun deleteReview(reviewId: Long) = mutate(
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

    fun likeReview(reviewId: Long, likedByMe: Boolean) = mutate {
        publicWrite(retryUnauthorized = !likedByMe) { credential ->
            if (likedByMe) repository.unlikeReview(reviewId, credential)
            else repository.likeReview(reviewId, credential)
        }
    }

    fun createReply(reviewId: Long, content: String) = mutate {
        publicWrite(retryUnauthorized = true) { repository.createReply(reviewId, content, it) }
    }

    fun updateReply(replyId: Long, content: String) = mutate(
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
    ) {
        publicWrite(retryUnauthorized = false) { repository.updateReply(replyId, content, it) }
    }

    fun deleteReply(replyId: Long) = mutate(
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

    fun likeReply(replyId: Long, likedByMe: Boolean) = mutate {
        publicWrite(retryUnauthorized = !likedByMe) { credential ->
            if (likedByMe) repository.unlikeReply(replyId, credential)
            else repository.likeReply(replyId, credential)
        }
    }

    private fun reload() {
        val book = _uiState.value.book ?: return
        val generation = requestGeneration
        val credential = readCredential()
        val reviewScope = _uiState.value.reviewScope
        val reviewSort = _uiState.value.reviewSort
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            runCatching {
                withDelayedLoading(
                    onLoadingChanged = { loading ->
                        if (generation == requestGeneration) {
                            _uiState.value = _uiState.value.copy(isLoading = loading)
                        }
                    },
                ) {
                    val detail = book.isbn13.takeIf(String::isNotBlank)
                        ?.let { repository.detail(it, credential) }
                    val reviews = detail?.bookId?.let { bookId ->
                        repository.reviews(
                            bookId,
                            reviewScope,
                            reviewSort,
                            credential,
                        )
                    }
                    detail to reviews
                }
            }.onSuccess { (detail, reviews) ->
                if (generation != requestGeneration) return@onSuccess
                _uiState.value = _uiState.value.copy(
                    detail = detail,
                    reviews = reviews?.items.orEmpty(),
                    reviewCount = reviews?.totalCount ?: 0,
                    nextReviewPage = reviews?.nextPage,
                )
            }.onFailure { error ->
                if (generation == requestGeneration || error is CancellationException) handleFailure(error)
            }
        }
    }

    private fun reloadReviews() {
        val bookId = _uiState.value.detail?.bookId ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            runCatching {
                withDelayedLoading(
                    onLoadingChanged = { loading ->
                        _uiState.value = _uiState.value.copy(isLoading = loading)
                    },
                ) {
                    repository.reviews(
                        bookId,
                        _uiState.value.reviewScope,
                        _uiState.value.reviewSort,
                        readCredential(),
                    )
                }
            }.onSuccess { reviews ->
                _uiState.value = _uiState.value.copy(
                    reviews = reviews.items,
                    reviewCount = reviews.totalCount,
                    nextReviewPage = reviews.nextPage,
                )
            }.onFailure(::handleFailure)
        }
    }

    private fun <T> mutate(
        onSuccess: (T) -> Unit = {},
        reloadAfterSuccess: Boolean = true,
        action: suspend () -> T,
    ) {
        if (mutationJob?.isActive == true) return
        val generation = requestGeneration
        mutationJob = viewModelScope.launch {
            runCatching {
                withDelayedLoading(
                    onLoadingChanged = { loading ->
                        if (generation == requestGeneration) {
                            _uiState.value = _uiState.value.copy(isSubmitting = loading)
                        }
                    },
                ) {
                    loadJob?.join()
                    action()
                }
            }.onSuccess { result ->
                if (generation != requestGeneration) return@onSuccess
                loadJob?.cancel()
                onSuccess(result)
                if (reloadAfterSuccess) reload()
            }.onFailure { error ->
                if (generation == requestGeneration || error is CancellationException) handleFailure(error)
            }
        }
    }

    private fun mutateLibraryRecord(
        onSuccess: () -> Unit,
        onLibraryAdded: () -> Unit,
        reloadAfterSuccess: Boolean = false,
        action: suspend (bookId: Long, token: String) -> LibraryRecord,
    ) {
        val generation = requestGeneration
        mutate(
            onSuccess = { record: LibraryRecord ->
                updateMyRecord(record)
                onSuccess()
            },
            reloadAfterSuccess = reloadAfterSuccess,
        ) {
            val token = requireToken()
            action(
                bookIdForWrite(token, generation, onLibraryAdded),
                token,
            )
        }
    }

    private suspend fun bookIdForWrite(
        token: String,
        generation: Int,
        onLibraryAdded: () -> Unit,
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
        return requireNotNull(record.bookId ?: _uiState.value.detail?.bookId)
    }

    private fun invalidateRequests() {
        requestGeneration += 1
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

    private fun bookIdForPublicWrite(): Long = requireNotNull(
        _uiState.value.detail?.bookId ?: _uiState.value.book?.bookId,
    )

    private suspend fun writeCredential(): WriteCredential = readCredential() ?: issueGuestCredential()

    private suspend fun issueGuestCredential(): WriteCredential.Guest {
        val guest = authRepository.issueGuest()
        authPlatform.writeGuest(guest)
        _uiState.value = _uiState.value.copy(guestNickname = guest.nickname)
        return WriteCredential.Guest(guest.token)
    }

    private suspend fun <T> publicWrite(
        retryUnauthorized: Boolean,
        request: suspend (WriteCredential) -> T,
    ): T {
        val credential = writeCredential()
        return try {
            request(credential)
        } catch (error: Throwable) {
            if (credential !is WriteCredential.Guest || !error.isUnauthorized()) throw error
            if (!retryUnauthorized) throw GuestOwnershipLostException()
            request(issueGuestCredential())
        }
    }

    private fun handleFailure(error: Throwable) {
        if (error is CancellationException) throw error
        _uiState.value = _uiState.value.copy(
            requestError = if (error is GuestOwnershipLostException) {
                "이 기기에서는 더 이상 수정할 수 없습니다."
            } else {
                "요청을 처리하지 못했어요. 다시 시도해 주세요."
            },
        )
    }
}

private class GuestOwnershipLostException : RuntimeException()

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
