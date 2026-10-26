package com.chaekchaek.app.ui.archive

import com.chaekchaek.app.data.remote.*
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class BookshelfFlowTest {
    private val book = BookDetailArgs("isbn", "isbn", title = "책")
    private fun shelf(id: Long, books: List<BookshelfBookSummary> = emptyList(), count: Int = books.size) = Bookshelf(id, "책장$id", id == 1L, count, books)

    @Test fun `담기 중 책장을 만들면 책과 입력 흐름을 보존하고 새 책장에 확정 저장한다`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = FakeShelves(listOf(shelf(1), shelf(2)))
            val vm = LibrarySaveViewModel({ _, _, _ -> error("legacy") }, { _, _ -> }, shelves = repo)
            vm.authenticate("a", 9); vm.save(book); advanceUntilIdle()
            vm.selectShelf(2); vm.beginShelfCreation(); vm.cancelShelfCreation()
            assertEquals(book, vm.uiState.value.choosingBook)
            assertEquals(2L, vm.uiState.value.selectedShelfId)
            vm.beginShelfCreation(); repo.fail = true
            vm.createShelf("새 책장"); advanceUntilIdle()
            assertTrue(vm.uiState.value.creatingShelf)
            assertEquals(book, vm.uiState.value.choosingBook)
            assertTrue(repo.added.isEmpty())
            repo.fail = false
            vm.errors.takeRecovery(vm.errors.notice.value!!.id)?.invoke(null); advanceUntilIdle()
            assertFalse(vm.uiState.value.creatingShelf)
            assertEquals(3L, vm.uiState.value.selectedShelfId)
            assertEquals("새 책장", vm.uiState.value.shelves.last().name)
            assertEquals(book, vm.uiState.value.choosingBook)
            assertTrue(repo.added.isEmpty())
            vm.confirmShelf(); advanceUntilIdle()
            assertEquals(listOf(3L), repo.added)
            assertNull(vm.uiState.value.choosingBook)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `책장 생성 응답이 늦어도 계정 변경 후 이전 선택을 복원하지 않는다`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = FakeShelves(listOf(shelf(1)))
            val vm = LibrarySaveViewModel({ _, _, _ -> error("legacy") }, { _, _ -> }, shelves = repo)
            vm.authenticate("a", 9); vm.save(book); advanceUntilIdle(); vm.beginShelfCreation()
            val gate = CompletableDeferred<Unit>(); repo.createGate = gate
            vm.createShelf("이전 책장"); runCurrent()
            vm.authenticate("b", 10); gate.complete(Unit); advanceUntilIdle()
            assertEquals(10L, vm.uiState.value.memberId)
            assertFalse(vm.uiState.value.creatingShelf)
            assertTrue(vm.uiState.value.shelves.isEmpty())
            assertNull(vm.uiState.value.choosingBook)
            assertTrue(repo.added.isEmpty())
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `최근 선택은 회원별 성공한 저장 뒤에만 갱신한다`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = FakeShelves(listOf(shelf(1), shelf(2)))
            val recent = mutableMapOf(9L to 2L)
            val vm = LibrarySaveViewModel({ _, _, _ -> error("기본 저장 경로 호출 금지") }, { _, _ -> }, shelves = repo,
                readRecentShelf = { recent[it] }, writeRecentShelf = { member, shelf -> recent[member] = shelf }, isSaved = { true })
            vm.authenticate("token")
            vm.save(book)
            advanceUntilIdle()
            assertEquals(2L, vm.uiState.value.selectedShelfId)
            assertTrue(repo.added.isEmpty())
            vm.selectShelf(1)
            repo.fail = true
            vm.confirmShelf()
            advanceUntilIdle()
            assertEquals(2L, recent[9L])
            assertEquals(book, vm.uiState.value.choosingBook)
            assertEquals(1L, vm.uiState.value.selectedShelfId)
            repo.fail = false
            vm.confirmShelf()
            advanceUntilIdle()
            assertEquals(1L, recent[9L])
            assertNull(vm.uiState.value.choosingBook)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `중복은 같은 책장에만 막고 30권 이상 책장의 추가를 막는다`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = FakeShelves(listOf(shelf(1, listOf(BookshelfBookSummary(42, "isbn", "책"))), shelf(2), shelf(3, count = 35)))
            val vm = LibrarySaveViewModel({ _, _, _ -> error("legacy") }, { _, _ -> }, shelves = repo)
            vm.authenticate("token"); vm.save(book); advanceUntilIdle()
            vm.confirmShelf(); advanceUntilIdle()
            assertTrue(repo.added.isEmpty())
            vm.selectShelf(3); vm.confirmShelf(); advanceUntilIdle()
            assertTrue(repo.added.isEmpty())
            vm.selectShelf(2); vm.confirmShelf(); advanceUntilIdle()
            assertEquals(listOf(2L), repo.added)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `삭제된 최근 책장은 기본 책장으로 대체하고 인증 취소는 저장하지 않는다`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = FakeShelves(listOf(shelf(2), shelf(1)))
            val vm = LibrarySaveViewModel({ _, _, _ -> error("legacy") }, { _, _ -> }, shelves = repo, readRecentShelf = { 99 })
            vm.save(book); assertEquals(book, vm.uiState.value.pendingBook)
            vm.cancelPendingSave(); vm.authenticate("token"); advanceUntilIdle()
            assertTrue(repo.added.isEmpty()); assertNull(vm.uiState.value.choosingBook)
            vm.save(book); advanceUntilIdle()
            assertEquals(1L, vm.uiState.value.selectedShelfId)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `순서 저장 실패는 기존 책장을 보존하고 재시도에서 전체 순서를 다시 보낸다`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = FakeShelves(listOf(shelf(1), shelf(2), shelf(3)))
            val vm = BookshelfViewModel(repo)
            vm.authenticate("token"); advanceUntilIdle()
            repo.fail = true
            vm.orderShelves(listOf(3, 1, 2)); advanceUntilIdle()
            assertEquals(listOf(1L, 2L, 3L), vm.uiState.value.shelves.map { it.libraryId })
            assertFalse(vm.uiState.value.mutating)
            assertNotNull(vm.errors.notice.value)
            repo.fail = false
            vm.errors.takeRecovery(vm.errors.notice.value!!.id)?.invoke(null)
            advanceUntilIdle()
            assertEquals(listOf(listOf(3L, 1L, 2L), listOf(3L, 1L, 2L)), repo.orders)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `배치 변경 뒤 상세 갱신이 끝날 때까지 기존 내용과 갱신 상태를 유지한다`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = FakeShelves(listOf(shelf(1)))
            val vm = BookshelfViewModel(repo)
            vm.authenticate("token"); advanceUntilIdle()
            vm.open(1); advanceUntilIdle()
            val previous = vm.uiState.value.detail
            val gate = CompletableDeferred<Unit>()
            repo.detailGate = gate
            vm.remove(1, 42); runCurrent()
            assertEquals(previous, vm.uiState.value.detail)
            assertTrue(vm.uiState.value.detailRefreshing)
            assertFalse(vm.uiState.value.refreshing)
            gate.complete(Unit); advanceUntilIdle()
            assertFalse(vm.uiState.value.detailRefreshing)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `변경 성공 후 재조회가 실패해도 이전 상세를 공유 가능한 상태로 바꾸지 않는다`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = FakeShelves(listOf(shelf(1)))
            val vm = BookshelfViewModel(repo)
            vm.authenticate("token"); advanceUntilIdle()
            vm.open(1); advanceUntilIdle()
            assertFalse(vm.uiState.value.detailStale)
            val previous = vm.uiState.value.detail
            repo.detailFails = true
            vm.remove(1, 42); advanceUntilIdle()
            assertEquals(previous, vm.uiState.value.detail)
            assertFalse(vm.uiState.value.detailRefreshing)
            assertTrue(vm.uiState.value.detailStale)
            repo.detailFails = false
            vm.errors.takeRecovery(vm.errors.notice.value!!.id)?.invoke(null)
            advanceUntilIdle()
            assertFalse(vm.uiState.value.detailStale)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `같은 회원 재인증은 선택을 보존하고 다른 회원은 목록과 담기 선택을 초기화한다`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = FakeShelves(listOf(shelf(1), shelf(2)))
            val vm = BookshelfViewModel(repo)
            val save = LibrarySaveViewModel({ _, _, _ -> error("legacy") }, { _, _ -> }, shelves = repo)
            vm.authenticate("a", 9); save.authenticate("a", 9); advanceUntilIdle()
            vm.open(1); save.save(book); advanceUntilIdle(); save.selectShelf(2)
            vm.authenticate("a-fresh", 9); save.authenticate("a-fresh", 9)
            assertEquals(1L, vm.uiState.value.selectedId)
            assertEquals(2L, save.uiState.value.selectedShelfId)
            val gate = CompletableDeferred<Unit>()
            repo.mineGate = gate; repo.memberId = 10
            vm.authenticate("b", 10); save.authenticate("b", 10); runCurrent()
            assertNull(vm.uiState.value.detail)
            assertNull(vm.uiState.value.selectedId)
            assertTrue(vm.uiState.value.shelves.isEmpty())
            assertNull(save.uiState.value.choosingBook)
            assertNull(save.uiState.value.selectedShelfId)
            assertEquals(10L, save.uiState.value.memberId)
            gate.complete(Unit); advanceUntilIdle()
            assertEquals(10L, vm.uiState.value.memberId)
            assertEquals(listOf("a", "a", "b"), repo.mineTokens)
            assertTrue(repo.added.isEmpty())
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `정렬 성공 뒤 목록과 상세 재조회 중 두 번째 정렬을 보내지 않는다`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = FakeShelves(listOf(shelf(1), shelf(2), shelf(3), shelf(4)))
            val vm = BookshelfViewModel(repo)
            vm.authenticate("a"); advanceUntilIdle()
            val gate = CompletableDeferred<Unit>()
            repo.mineGate = gate
            vm.orderShelves(listOf(2, 1, 3, 4)); runCurrent()
            assertFalse(vm.uiState.value.mutating)
            assertTrue(vm.uiState.value.refreshing)
            vm.orderShelves(listOf(1, 2, 4, 3)); runCurrent()
            vm.orderBooks(1, emptyList()); runCurrent()
            assertEquals(listOf(listOf(2L, 1L, 3L, 4L)), repo.orders)
            assertTrue(repo.bookOrders.isEmpty())
            gate.complete(Unit); advanceUntilIdle()
            vm.open(1); advanceUntilIdle()
            val detailGate = CompletableDeferred<Unit>()
            repo.detailGate = detailGate
            vm.orderBooks(1, emptyList()); runCurrent()
            assertTrue(vm.uiState.value.detailRefreshing)
            vm.orderBooks(1, emptyList()); runCurrent()
            assertEquals(1, repo.bookOrders.size)
            detailGate.complete(Unit); advanceUntilIdle()
            vm.orderBooks(1, emptyList()); advanceUntilIdle()
            assertEquals(2, repo.bookOrders.size)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `정렬 성공 후 목록 조회 실패에서도 이전 순서로 다음 정렬을 보내지 않는다`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = FakeShelves(listOf(shelf(1), shelf(2), shelf(3)))
            val vm = BookshelfViewModel(repo)
            vm.authenticate("a"); advanceUntilIdle()
            repo.mineFails = true
            vm.orderShelves(listOf(2, 1, 3)); advanceUntilIdle()
            assertTrue(vm.uiState.value.collectionStale)
            vm.orderShelves(listOf(1, 3, 2)); advanceUntilIdle()
            assertEquals(listOf(listOf(2L, 1L, 3L)), repo.orders)
            repo.mineFails = false
            vm.errors.takeRecovery(vm.errors.notice.value!!.id)?.invoke(null); advanceUntilIdle()
            assertFalse(vm.uiState.value.collectionStale)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `이름과 순서는 기존 긴 이름과 35권 이전 데이터를 임의로 자르지 않는다`() {
        assertNotNull(bookshelfNameError(" ", emptyList()))
        assertNotNull(bookshelfNameError("12345678901", emptyList()))
        assertNotNull(bookshelfNameError("BOOK", listOf("book")))
        assertNull(bookshelfNameError("가나다라마바아자차카", emptyList()))
        assertTrue(sameIds((1L..35).reversed().toList(), (1L..35).toList()))
        assertFalse(sameIds(listOf(1, 1), listOf(1, 2)))
        assertFalse(sameIds(listOf(1), listOf(1, 2)))
        assertTrue(sameIds(emptyList(), emptyList()))
    }
}

private class FakeShelves(val shelves: List<Bookshelf>) : BookshelfRepository {
    var fail = false
    var createGate: CompletableDeferred<Unit>? = null
    var detailGate: CompletableDeferred<Unit>? = null
    var detailFails = false
    var mineFails = false
    var memberId = 9L
    var mineGate: CompletableDeferred<Unit>? = null
    val mineTokens = mutableListOf<String>()
    val bookOrders = mutableListOf<List<Long>>()
    val added = mutableListOf<Long>()
    val orders = mutableListOf<List<Long>>()
    override suspend fun mine(token: String): BookshelfCollection {
        mineTokens += token
        mineGate?.await()
        if (mineFails) error("offline")
        return BookshelfCollection(memberId, shelves)
    }
    override suspend fun add(libraryId: Long, isbn13: String, totalPages: Int?, token: String): LibraryRecord {
        if (fail) error("offline")
        added += libraryId
        return LibraryRecord("FINISHED", 100, 4.5, 42)
    }
    override suspend fun detail(libraryId: Long): BookshelfDetail {
        detailGate?.await()
        if (detailFails) error("offline")
        return BookshelfDetail(libraryId, 9, "책장", false, 0, emptyList())
    }
    override suspend fun create(name: String, token: String): Bookshelf {
        withContext(NonCancellable) { createGate?.await() }
        if (fail) error("offline")
        return Bookshelf(3, name, false, 0, emptyList())
    }
    override suspend fun rename(libraryId: Long, name: String, token: String) {}
    override suspend fun delete(libraryId: Long, token: String) {}
    override suspend fun remove(libraryId: Long, bookId: Long, token: String) {}
    override suspend fun orderShelves(ids: List<Long>, token: String) { orders += ids; if (fail) error("offline") }
    override suspend fun orderBooks(libraryId: Long, ids: List<Long>, token: String) { bookOrders += ids; if (fail) error("offline") }
}
