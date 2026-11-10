package com.chaekchaek.app.deeplink

import androidx.lifecycle.SavedStateHandle
import com.chaekchaek.app.data.remote.BookDetail
import com.chaekchaek.app.data.remote.MobileLoginException
import com.chaekchaek.app.presentation.common.ErrorAction
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class BookDeepLinkViewModelTest {
    private val isbn = "9788937460449"
    private val other = "9791172376215"
    private fun scenario(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block() } finally { Dispatchers.resetMain() }
    }
    private fun delivery(isbn: String? = this.isbn) = BookDeepLinkDelivery(newDeliveryId(), isbn)
    private fun book(isbn: String = this.isbn) = BookDetail(1, isbn, "테스트 책", listOf("저자"), emptyList(), "출판사", "소설", "2026-01-01", 200, "", 0, null, 0, null)

    @Test fun sameBookRetainsItsEntryWithoutFetching() = scenario {
        var requests = 0
        val vm = BookDeepLinkViewModel(SavedStateHandle(), { requests++; book(it) })
        vm.accept(delivery())
        vm.openPending(isbn, { true }) { fail("기존 상세를 다시 열면 안 된다") }
        runCurrent()
        assertEquals(0, requests); assertNull(vm.pending.value)
    }

    @Test fun pendingLinkSurvivesOnboardingOrProcessRestorationUntilOpened() = scenario {
        val saved = SavedStateHandle()
        val first = BookDeepLinkViewModel(saved, { fail("준비 전에 조회하면 안 된다") })
        first.accept(delivery())
        val restored = SavedStateHandle(mapOf("pending_book_deep_link" to first.pending.value))
        val second = BookDeepLinkViewModel(restored, { book(it) })
        var opened: BookDetailArgs? = null
        second.openPending(null, { true }) { opened = it }; runCurrent()
        assertEquals(isbn, opened?.isbn13); assertNull(second.pending.value)
    }

    @Test fun malformedLinkShowsConfirmWithoutRequestOrNavigation() = scenario {
        val vm = BookDeepLinkViewModel(SavedStateHandle(), { fail("잘못된 링크를 조회하면 안 된다") })
        vm.accept(delivery(null)); vm.openPending(null, { true }) { fail("잘못된 링크를 열면 안 된다") }
        assertEquals("링크를 열 수 없어요", vm.errors.notice.value?.title)
        assertEquals(ErrorAction.Confirm, vm.errors.notice.value?.action)
        vm.errors.dismiss(); assertNull(vm.errors.notice.value)
    }

    @Test fun missingBookDoesNotChangeScreen() = scenario {
        for (status in listOf(404, 410)) {
            val vm = BookDeepLinkViewModel(SavedStateHandle(), { throw MobileLoginException("NOT_FOUND", status) })
            vm.accept(delivery()); vm.openPending(null, { true }) { fail("없는 책을 열면 안 된다") }; runCurrent()
            assertEquals("책을 찾을 수 없어요", vm.errors.notice.value?.title)
            assertEquals(ErrorAction.Confirm, vm.errors.notice.value?.action)
        }
    }

    @Test fun networkFailureCanRetrySameTargetAndDoesNotRequireLogin() = scenario {
        var requests = 0
        var opened: String? = null
        val vm = BookDeepLinkViewModel(SavedStateHandle(), { if (++requests == 1) error("offline") else book(it) })
        vm.accept(delivery()); vm.openPending(null, { true }) { opened = it.isbn13 }; runCurrent()
        val notice = assertNotNull(vm.errors.notice.value)
        assertEquals(ErrorAction.Retry, notice.action)
        vm.errors.takeRecovery(notice.id)?.invoke(null)
        vm.openPending(null, { true }) { opened = it.isbn13 }; runCurrent()
        assertEquals(isbn, opened); assertEquals(2, requests)
    }

    @Test fun latestDeliveryCancelsOldRequestEvenIfOldRepositoryIgnoresCancellation() = scenario {
        val old = CompletableDeferred<BookDetail>()
        val opened = mutableListOf<String>()
        val vm = BookDeepLinkViewModel(SavedStateHandle(), { if (it == isbn) withContext(NonCancellable) { old.await() } else book(it) })
        vm.accept(delivery()); vm.openPending(null, { true }) { opened += it.isbn13 }; runCurrent()
        vm.accept(delivery(other)); vm.openPending(null, { true }) { opened += it.isbn13 }; runCurrent()
        old.complete(book()); runCurrent()
        assertEquals(listOf(other), opened); assertNull(vm.errors.notice.value)
    }

    @Test fun delayedResultCannotOpenOverAnotherScreenAndLoadingStartsAfter500ms() = scenario {
        val response = CompletableDeferred<BookDetail>()
        var current = true
        val vm = BookDeepLinkViewModel(SavedStateHandle(), { response.await() })
        vm.accept(delivery()); vm.openPending(null, { current }) { fail("원래 화면을 떠난 뒤 열면 안 된다") }
        runCurrent(); advanceTimeBy(499); runCurrent(); assertFalse(vm.showLoading.value)
        advanceTimeBy(1); runCurrent(); assertTrue(vm.showLoading.value)
        current = false; response.complete(book()); runCurrent()
        assertFalse(vm.showLoading.value); assertNull(vm.pending.value)
    }

    @Test fun oldRetryIsInvalidatedByNewDelivery() = scenario {
        val vm = BookDeepLinkViewModel(SavedStateHandle(), { error("offline") })
        vm.accept(delivery()); vm.openPending(null, { true }) {}; runCurrent()
        val oldId = assertNotNull(vm.errors.notice.value).id
        val next = delivery(other); vm.accept(next)
        assertNull(vm.errors.takeRecovery(oldId))
        assertTrue(assertNotNull(vm.pending.value).endsWith(other))
    }
}
