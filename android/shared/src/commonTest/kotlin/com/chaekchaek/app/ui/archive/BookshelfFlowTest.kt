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
    var detailGate: CompletableDeferred<Unit>? = null
    val added = mutableListOf<Long>()
    val orders = mutableListOf<List<Long>>()
    override suspend fun mine(token: String) = BookshelfCollection(9, shelves)
    override suspend fun add(libraryId: Long, isbn13: String, totalPages: Int?, token: String): LibraryRecord {
        if (fail) error("offline")
        added += libraryId
        return LibraryRecord("FINISHED", 100, 4.5, 42)
    }
    override suspend fun detail(libraryId: Long): BookshelfDetail {
        detailGate?.await()
        return BookshelfDetail(libraryId, 9, "책장", false, 0, emptyList())
    }
    override suspend fun create(name: String, token: String) = Bookshelf(3, name, false, 0, emptyList())
    override suspend fun rename(libraryId: Long, name: String, token: String) {}
    override suspend fun delete(libraryId: Long, token: String) {}
    override suspend fun remove(libraryId: Long, bookId: Long, token: String) {}
    override suspend fun orderShelves(ids: List<Long>, token: String) { orders += ids; if (fail) error("offline") }
    override suspend fun orderBooks(libraryId: Long, ids: List<Long>, token: String) { if (fail) error("offline") }
}
