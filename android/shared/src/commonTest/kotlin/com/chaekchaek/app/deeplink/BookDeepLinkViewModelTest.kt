package com.chaekchaek.app.deeplink

import androidx.lifecycle.SavedStateHandle
import com.chaekchaek.app.data.remote.BookDetail
import com.chaekchaek.app.data.remote.MobileLoginException
import com.chaekchaek.app.presentation.common.ErrorAction
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
    private fun book(isbn: String = this.isbn) = BookDetail(
        bookId = 1, isbn13 = isbn, title = "테스트 책", authors = listOf("저자"),
        translators = emptyList(), publisher = "출판사", category = "소설",
        publishedDate = "2026-01-01", coverImageUrl = "", commentCount = 0,
        averageRating = null, ratingCount = 0, myRecord = null,
    )

    private fun TestScope.startPending(
        vm: BookDeepLinkViewModel,
        currentIsbn: String? = null,
        sourceIsCurrent: () -> Boolean = { true },
        readyToOpen: () -> Boolean = { true },
        openBook: (BookDetail) -> Unit,
    ) = launch { vm.openPending(currentIsbn, sourceIsCurrent = sourceIsCurrent, readyToOpen = readyToOpen, openBook = openBook) }

    @Test fun sameBookRetainsItsEntryWithoutFetching() = scenario {
        var requests = 0
        val vm = BookDeepLinkViewModel(SavedStateHandle(), { isbn, _ -> requests++; book(isbn) })
        vm.accept(delivery())
        startPending(vm, isbn) { fail("기존 상세를 다시 열면 안 된다") }
        runCurrent()
        assertEquals(0, requests); assertNull(vm.pending.value)
    }

    @Test fun pendingLinkSurvivesOnboardingOrProcessRestorationUntilOpened() = scenario {
        val saved = SavedStateHandle()
        val first = BookDeepLinkViewModel(saved, { _, _ -> fail("준비 전에 조회하면 안 된다") })
        first.accept(delivery())
        val restored = SavedStateHandle(mapOf("pending_book_deep_link" to first.pending.value))
        val second = BookDeepLinkViewModel(restored, { isbn, _ -> book(isbn) })
        var opened: BookDetail? = null
        startPending(second) { opened = it }; runCurrent()
        assertEquals(isbn, opened?.isbn13); assertNull(second.pending.value)
    }

    @Test fun malformedLinkShowsConfirmWithoutRequestOrNavigation() = scenario {
        val vm = BookDeepLinkViewModel(SavedStateHandle(), { _, _ -> fail("잘못된 링크를 조회하면 안 된다") })
        vm.accept(delivery(null)); startPending(vm) { fail("잘못된 링크를 열면 안 된다") }; runCurrent()
        assertEquals("링크를 열 수 없어요", vm.errors.notice.value?.title)
        assertEquals(ErrorAction.Confirm, vm.errors.notice.value?.action)
        vm.errors.dismiss(); assertNull(vm.errors.notice.value)
    }

    @Test fun missingBookDoesNotChangeScreen() = scenario {
        for (status in listOf(404, 410)) {
            val vm = BookDeepLinkViewModel(SavedStateHandle(), { _, _ -> throw MobileLoginException("NOT_FOUND", status) })
            vm.accept(delivery()); startPending(vm) { fail("없는 책을 열면 안 된다") }; runCurrent()
            assertEquals("책을 찾을 수 없어요", vm.errors.notice.value?.title)
            assertEquals(ErrorAction.Confirm, vm.errors.notice.value?.action)
        }
    }

    @Test fun networkFailureCanRetrySameTargetAndDoesNotRequireLogin() = scenario {
        var requests = 0
        var opened: String? = null
        val vm = BookDeepLinkViewModel(SavedStateHandle(), { isbn, _ -> if (++requests == 1) error("offline") else book(isbn) })
        vm.accept(delivery()); startPending(vm) { opened = it.isbn13 }; runCurrent()
        val notice = assertNotNull(vm.errors.notice.value)
        assertEquals(ErrorAction.Retry, notice.action)
        vm.errors.takeRecovery(notice.id)?.invoke(null)
        startPending(vm) { opened = it.isbn13 }; runCurrent()
        assertEquals(isbn, opened); assertEquals(2, requests)
    }

    @Test fun latestDeliveryCancelsOldRequestEvenIfOldRepositoryIgnoresCancellation() = scenario {
        val old = CompletableDeferred<BookDetail>()
        val opened = mutableListOf<String>()
        val vm = BookDeepLinkViewModel(SavedStateHandle(), { target, _ -> if (target == isbn) withContext(NonCancellable) { old.await() } else book(target) })
        vm.accept(delivery())
        val oldRequest = startPending(vm) { opened += it.isbn13 }; runCurrent()
        oldRequest.cancel()
        vm.accept(delivery(other)); startPending(vm) { opened += it.isbn13 }; runCurrent()
        old.complete(book()); runCurrent()
        assertEquals(listOf(other), opened); assertNull(vm.errors.notice.value)
    }

    @Test fun unreadPlatformDeliveryPreventsOlderLookupFromOpening() = scenario {
        val inbox = BookDeepLinkInbox()
        val response = CompletableDeferred<BookDetail>()
        val vm = BookDeepLinkViewModel(SavedStateHandle(), { target, _ -> if (target == isbn) response.await() else book(target) })
        inbox.submit(BookDeepLink.exampleUri)
        vm.accept(assertNotNull(inbox.pending.value))
        val ready = { !vm.hasUnreadDelivery(inbox.pending.value) }
        assertTrue(ready())
        startPending(vm, readyToOpen = ready) { fail("최신 링크가 수신되면 이전 책을 열면 안 된다") }
        runCurrent()
        inbox.submit("chaekchaek://books/$other")
        response.complete(book())
        runCurrent()
        val next = assertNotNull(inbox.pending.value)
        vm.accept(next)
        inbox.acknowledge(next.id)
        var opened: String? = null
        startPending(vm, readyToOpen = ready) { opened = it.isbn13 }
        runCurrent()
        assertEquals(other, opened)
    }

    @Test fun delayedResultCannotOpenOverAnotherScreenAndLoadingStartsAfter500ms() = scenario {
        val response = CompletableDeferred<BookDetail>()
        var current = true
        val vm = BookDeepLinkViewModel(SavedStateHandle(), { _, _ -> response.await() })
        vm.accept(delivery()); startPending(vm, sourceIsCurrent = { current }) { fail("원래 화면을 떠난 뒤 열면 안 된다") }
        runCurrent(); advanceTimeBy(499); runCurrent(); assertFalse(vm.showLoading.value)
        advanceTimeBy(1); runCurrent(); assertTrue(vm.showLoading.value)
        current = false; response.complete(book()); runCurrent()
        assertFalse(vm.showLoading.value); assertNull(vm.pending.value)
    }

    @Test fun canceledLookupLeavesPendingLinkForTheNewScreen() = scenario {
        val response = CompletableDeferred<BookDetail>()
        val vm = BookDeepLinkViewModel(SavedStateHandle(), { _, _ -> response.await() })
        vm.accept(delivery())
        val first = startPending(vm) { fail("이전 화면에 상세를 열면 안 된다") }
        runCurrent()
        first.cancelAndJoin()
        assertNotNull(vm.pending.value)
        var opened: BookDetail? = null
        startPending(vm) { opened = it }
        response.complete(book())
        runCurrent()
        assertEquals(isbn, opened?.isbn13)
        assertNull(vm.pending.value)
    }

    @Test fun lookupFinishingDuringLibrarySaveWaitsToOpen() = scenario {
        val response = CompletableDeferred<BookDetail>()
        val vm = BookDeepLinkViewModel(SavedStateHandle(), { _, _ -> response.await() })
        vm.accept(delivery())
        var saving = false
        startPending(vm, readyToOpen = { !saving }) { fail("담기가 끝나기 전에 링크를 열면 안 된다") }
        runCurrent()
        saving = true
        response.complete(book())
        runCurrent()
        assertNotNull(vm.pending.value)
        assertNull(vm.errors.notice.value)

        saving = false
        var opened: BookDetail? = null
        startPending(vm, readyToOpen = { !saving }) { opened = it }
        runCurrent()
        assertEquals(isbn, opened?.isbn13)
        assertNull(vm.pending.value)
    }

    @Test fun oldRetryIsInvalidatedByNewDelivery() = scenario {
        val vm = BookDeepLinkViewModel(SavedStateHandle(), { _, _ -> error("offline") })
        vm.accept(delivery()); startPending(vm) {}; runCurrent()
        val oldId = assertNotNull(vm.errors.notice.value).id
        val next = delivery(other); vm.accept(next)
        assertNull(vm.errors.takeRecovery(oldId))
        assertTrue(assertNotNull(vm.pending.value).endsWith(other))
    }
}
