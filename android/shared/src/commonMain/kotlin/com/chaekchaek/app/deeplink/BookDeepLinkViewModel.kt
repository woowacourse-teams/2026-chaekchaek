package com.chaekchaek.app.deeplink

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.analytics.UserErrorReporter
import com.chaekchaek.app.data.remote.BookDetail
import com.chaekchaek.app.data.remote.MobileLoginException
import com.chaekchaek.app.data.remote.WriteCredential
import com.chaekchaek.app.presentation.common.ErrorOperation
import com.chaekchaek.app.presentation.common.ErrorRecoveryState
import com.chaekchaek.app.presentation.common.withDelayedApiLoading
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.coroutineContext

internal class BookDeepLinkViewModel(
    private val savedState: SavedStateHandle,
    private val lookup: suspend (String, WriteCredential?) -> BookDetail,
    errorReporter: UserErrorReporter = UserErrorReporter.None,
) : ViewModel() {
    val pending = savedState.getStateFlow<String?>(PENDING, null)
    val errors = ErrorRecoveryState(errorReporter)
    private val loading = MutableStateFlow(false)
    val showLoading = loading.asStateFlow()
    private var activeLookup: Any? = null
    private var sharedLookup: SharedLookup? = null

    /** 링크 하나의 조회. 화면 효과가 다시 시작돼도 같은 링크와 자격 증명이면 진행 중인 요청을 기다린다. */
    private class SharedLookup(val request: String, val credential: WriteCredential?, val result: Deferred<BookDetail>)

    fun accept(delivery: BookDeepLinkDelivery) {
        activeLookup = null
        releaseLookup()
        loading.value = false
        errors.dismiss()
        savedState[PENDING] = "${delivery.id}|${delivery.isbn13.orEmpty()}"
    }

    fun hasUnreadDelivery(delivery: BookDeepLinkDelivery?): Boolean =
        delivery != null && delivery.id != pending.value?.substringBefore('|')

    /** 준비 전에는 호출하지 않는다. 조회 중 다른 화면으로 이동했다면 뒤늦게 상세를 열지 않는다. */
    suspend fun openPending(
        currentIsbn: String?,
        credential: WriteCredential? = null,
        sourceIsCurrent: () -> Boolean,
        readyToOpen: () -> Boolean = { true },
        openBook: (BookDetail) -> Unit,
    ) {
        val request = pending.value ?: return
        if (!readyToOpen()) return
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
        val lookupIdentity = Any()
        activeLookup = lookupIdentity
        try {
            val book = withDelayedApiLoading(
                onLoadingChanged = { if (activeLookup === lookupIdentity) loading.value = it },
            ) { lookupOnce(request, isbn, credential).await() }
            coroutineContext.ensureActive()
            if (pending.value != request || !readyToOpen()) return
            consume(request)
            if (!sourceIsCurrent()) return
            if (book.isbn13 != isbn) {
                errors.confirm("책 링크를 확인한 뒤 다시 열어 주세요.", "책을 찾을 수 없어요")
                return
            }
            openBook(book)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            coroutineContext.ensureActive()
            if (pending.value != request || !readyToOpen()) return
            consume(request)
            if (!sourceIsCurrent()) return
            val status = (error as? ResponseException)?.response?.status?.value
                ?: (error as? MobileLoginException)?.statusCode
            when {
                status == 404 || status == 410 ->
                    errors.confirm("삭제되었거나 더 이상 볼 수 없는 책이에요.", "책을 찾을 수 없어요")
                status == 403 || status == 401 && credential !is WriteCredential.Member ->
                    errors.confirm("이 책을 볼 수 없어요.", "책을 열 수 없어요")
                else -> errors.report(error, ErrorOperation.Request) {
                    if (sourceIsCurrent()) accept(BookDeepLinkDelivery(newDeliveryId(), isbn))
                }
            }
        } finally {
            if (activeLookup === lookupIdentity) {
                activeLookup = null
                loading.value = false
            }
        }
    }

    private fun lookupOnce(request: String, isbn: String, credential: WriteCredential?): Deferred<BookDetail> {
        sharedLookup?.takeIf { it.request == request && it.credential == credential && !it.result.isCancelled }
            ?.let { return it.result }
        releaseLookup()
        // 화면 효과의 수명이 아니라 ViewModel 수명에 묶어, 효과가 취소돼도 요청을 다시 보내지 않는다.
        return viewModelScope.async { lookup(isbn, credential) }
            .also { sharedLookup = SharedLookup(request, credential, it) }
    }

    private fun releaseLookup() {
        sharedLookup?.result?.cancel()
        sharedLookup = null
    }

    private fun consume(request: String) {
        if (sharedLookup?.request == request) sharedLookup = null
        if (pending.value == request) savedState[PENDING] = null
    }

    private companion object { const val PENDING = "pending_book_deep_link" }
}
