package com.chaekchaek.app.deeplink

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.analytics.UserErrorReporter
import com.chaekchaek.app.data.remote.BookDetail
import com.chaekchaek.app.data.remote.MobileLoginException
import com.chaekchaek.app.presentation.common.ErrorOperation
import com.chaekchaek.app.presentation.common.ErrorRecoveryState
import com.chaekchaek.app.presentation.common.withDelayedApiLoading
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

internal class BookDeepLinkViewModel(
    private val savedState: SavedStateHandle,
    private val lookup: suspend (String) -> BookDetail,
    errorReporter: UserErrorReporter = UserErrorReporter.None,
) : ViewModel() {
    val pending = savedState.getStateFlow<String?>(PENDING, null)
    val errors = ErrorRecoveryState(errorReporter)
    private val loading = MutableStateFlow(false)
    val showLoading = loading.asStateFlow()
    private var requestJob: Job? = null
    private var activeRequest: String? = null

    fun accept(delivery: BookDeepLinkDelivery) {
        requestJob?.cancel()
        activeRequest = null
        loading.value = false
        errors.dismiss()
        savedState[PENDING] = "${delivery.id}|${delivery.isbn13.orEmpty()}"
    }

    /** 준비 전에는 호출하지 않는다. 조회 중 다른 화면으로 이동했다면 뒤늦게 상세를 열지 않는다. */
    fun openPending(
        currentIsbn: String?,
        sourceIsCurrent: () -> Boolean,
        openBook: (BookDetailArgs) -> Unit,
    ) {
        val request = pending.value ?: return
        if (activeRequest == request) return
        val isbn = BookDeepLink.isbn13("chaekchaek://books/${request.substringAfter('|', "")}")
        if (isbn == null) {
            consume(request)
            errors.confirm("책 링크를 확인한 뒤 다시 열어 주세요.", "링크를 열 수 없어요")
            return
        }
        if (currentIsbn == isbn) {
            consume(request)
            return
        }
        activeRequest = request
        requestJob = viewModelScope.launch {
            try {
                val book = withDelayedApiLoading({ if (activeRequest == request) loading.value = it }) { lookup(isbn) }
                coroutineContext.ensureActive()
                if (pending.value != request) return@launch
                consume(request)
                if (!sourceIsCurrent()) return@launch
                if (book.isbn13 != isbn) {
                    errors.confirm("책 링크를 확인한 뒤 다시 열어 주세요.", "책을 찾을 수 없어요")
                    return@launch
                }
                openBook(BookDetailArgs(
                    id = isbn, isbn13 = isbn, bookId = book.bookId, title = book.title,
                    creator = (book.authors + book.translators.map { "$it 옮김" }).joinToString("  "),
                    publisher = book.publisher, year = book.publishedDate?.take(4).orEmpty(),
                    category = book.category, totalPages = book.totalPages ?: 0, coverUrl = book.coverImageUrl,
                ))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (pending.value != request) return@launch
                consume(request)
                if (!sourceIsCurrent()) return@launch
                val status = (error as? ResponseException)?.response?.status?.value
                    ?: (error as? MobileLoginException)?.statusCode
                when (status) {
                    404, 410 -> errors.confirm("삭제되었거나 더 이상 볼 수 없는 책이에요.", "책을 찾을 수 없어요")
                    401, 403 -> errors.confirm("이 책을 볼 수 없어요.", "책을 열 수 없어요")
                    else -> errors.report(error, ErrorOperation.Request) {
                        if (sourceIsCurrent()) accept(BookDeepLinkDelivery(newDeliveryId(), isbn))
                    }
                }
            } finally {
                if (activeRequest == request) {
                    activeRequest = null
                    loading.value = false
                }
            }
        }
    }

    private fun consume(request: String) {
        if (pending.value == request) savedState[PENDING] = null
    }

    private companion object { const val PENDING = "pending_book_deep_link" }
}
