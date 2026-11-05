package com.chaekchaek.app.ui.archive

import com.chaekchaek.app.data.remote.Bookshelf
import com.chaekchaek.app.data.remote.BookshelfBookSummary
import com.chaekchaek.app.data.remote.BookshelfCollection
import com.chaekchaek.app.data.remote.BookshelfDetail
import com.chaekchaek.app.data.remote.BookshelfRepository
import com.chaekchaek.app.data.remote.LibraryRecord
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryTargetShelfSaveTest {
    private val book = BookDetailArgs("isbn", "isbn", title = "책")
    private fun shelf(id: Long, count: Int = 0, items: List<BookshelfBookSummary> = emptyList()) =
        Bookshelf(id, "책장$id", id == 1L, count, items)

    @Test fun `출발 책장은 최근 선택과 달라도 선택창 없이 바로 저장한다`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = TargetShelves(listOf(shelf(1), shelf(2)))
            val recent = mutableListOf<Pair<Long, Long>>()
            val vm = LibrarySaveViewModel({ _, _, _ -> error("기본 경로 호출 금지") }, { _, _ -> },
                shelves = repo, readRecentShelf = { 1 }, writeRecentShelf = { member, id -> recent += member to id })
            vm.authenticate("token", 9)
            vm.saveToShelf(book, 2); advanceUntilIdle()
            assertEquals(listOf(2L to "token"), repo.added)
            assertEquals(listOf(9L to 2L), recent)
            assertNull(vm.uiState.value.choosingBook)
            assertEquals(1, vm.uiState.value.savedCount)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `저장 실패와 새 토큰 재시도는 출발 책장을 유지한다`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = TargetShelves(listOf(shelf(1), shelf(2)))
            repo.addFails = true
            val vm = LibrarySaveViewModel({ _, _, _ -> error("기본 경로 호출 금지") }, { _, _ -> }, shelves = repo)
            vm.authenticate("old", 9); vm.saveToShelf(book, 2); advanceUntilIdle()
            assertEquals(book, vm.uiState.value.failedBook)
            assertEquals(0, vm.uiState.value.savedCount)
            repo.addFails = false
            vm.errors.takeRecovery(vm.errors.notice.value!!.id)?.invoke("fresh"); advanceUntilIdle()
            assertEquals(listOf(2L to "old", 2L to "fresh"), repo.added)
            assertEquals(1, vm.uiState.value.savedCount)
            assertNull(vm.uiState.value.choosingBook)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `출발 책장 조회 실패도 재시도에서 다른 책장으로 바꾸지 않는다`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = TargetShelves(listOf(shelf(1), shelf(2)))
            repo.mineFails = true
            val vm = LibrarySaveViewModel({ _, _, _ -> error("기본 경로 호출 금지") }, { _, _ -> }, shelves = repo)
            vm.authenticate("token", 9); vm.saveToShelf(book, 2); advanceUntilIdle()
            assertTrue(repo.added.isEmpty())
            repo.mineFails = false
            vm.errors.takeRecovery(vm.errors.notice.value!!.id)?.invoke(null); advanceUntilIdle()
            assertEquals(listOf(2L to "token"), repo.added)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `삭제되거나 가득 찼거나 중복된 출발 책장을 다른 책장으로 대체하지 않는다`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val duplicate = BookshelfBookSummary(42, "isbn", "책", "cover")
            for (targets in listOf(emptyList(), listOf(shelf(2, 30)), listOf(shelf(2, 1, listOf(duplicate))))) {
                val repo = TargetShelves(listOf(shelf(1)) + targets)
                val vm = LibrarySaveViewModel({ _, _, _ -> error("기본 경로 호출 금지") }, { _, _ -> }, shelves = repo)
                vm.authenticate("token", 9); vm.saveToShelf(book, 2); advanceUntilIdle()
                assertTrue(repo.added.isEmpty())
                assertNotNull(vm.errors.notice.value)
                assertNull(vm.uiState.value.choosingBook)
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `로그인 대기는 출발 책장을 보존하고 취소하면 다음 로그인에서 저장하지 않는다`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = TargetShelves(listOf(shelf(1), shelf(2)))
            val vm = LibrarySaveViewModel({ _, _, _ -> error("기본 경로 호출 금지") }, { _, _ -> }, shelves = repo)
            vm.saveToShelf(book, 2)
            assertEquals(2L, vm.uiState.value.pendingShelfId)
            vm.authenticate("first", 9); advanceUntilIdle()
            assertEquals(listOf(2L to "first"), repo.added)
            vm.authenticate(null)
            vm.saveToShelf(book, 2); vm.cancelPendingSave()
            assertNull(vm.uiState.value.pendingShelfId)
            vm.authenticate("second", 9); advanceUntilIdle()
            assertEquals(1, repo.added.size)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `다른 계정으로 바뀐 뒤 이전 실패 콜백은 출발 책장 저장을 실행하지 않는다`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = TargetShelves(listOf(shelf(1), shelf(2)))
            repo.addFails = true
            val vm = LibrarySaveViewModel({ _, _, _ -> error("기본 경로 호출 금지") }, { _, _ -> }, shelves = repo)
            vm.authenticate("first", 9); vm.saveToShelf(book, 2); advanceUntilIdle()
            val recovery = vm.errors.takeRecovery(vm.errors.notice.value!!.id)
            assertNotNull(recovery)
            vm.authenticate("second", 10); repo.addFails = false
            recovery("stale"); advanceUntilIdle()
            assertEquals(listOf(2L to "first"), repo.added)
            assertEquals(10L, vm.uiState.value.memberId)
            assertEquals(0, vm.uiState.value.savedCount)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `출발 책장 조회 중 계정 변경은 이전 저장을 취소한다`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = TargetShelves(listOf(shelf(1), shelf(2)))
            val gate = CompletableDeferred<Unit>(); repo.mineGate = gate
            val vm = LibrarySaveViewModel({ _, _, _ -> error("기본 경로 호출 금지") }, { _, _ -> }, shelves = repo)
            vm.authenticate("first", 9); vm.saveToShelf(book, 2); runCurrent()
            vm.authenticate("second", 10); gate.complete(Unit); advanceUntilIdle()
            assertTrue(repo.added.isEmpty())
            assertNull(vm.uiState.value.savingIsbn)
            assertEquals(10L, vm.uiState.value.memberId)
        } finally { Dispatchers.resetMain() }
    }
}

private class TargetShelves(private val libraries: List<Bookshelf>) : BookshelfRepository {
    var mineFails = false
    var addFails = false
    var mineGate: CompletableDeferred<Unit>? = null
    val added = mutableListOf<Pair<Long, String>>()
    override suspend fun mine(token: String): BookshelfCollection {
        mineGate?.await()
        check(!mineFails) { "조회 실패" }
        return BookshelfCollection(9, libraries)
    }
    override suspend fun add(libraryId: Long, isbn13: String, totalPages: Int?, token: String): LibraryRecord {
        added += libraryId to token
        check(!addFails) { "저장 실패" }
        return LibraryRecord("READING", 0, null, 42)
    }
    override suspend fun detail(libraryId: Long): BookshelfDetail = error("사용하지 않는 경로")
    override suspend fun create(name: String, token: String): Bookshelf = error("사용하지 않는 경로")
    override suspend fun rename(libraryId: Long, name: String, token: String) = Unit
    override suspend fun delete(libraryId: Long, token: String) = Unit
    override suspend fun remove(libraryId: Long, bookId: Long, token: String) = Unit
    override suspend fun orderShelves(ids: List<Long>, token: String) = Unit
    override suspend fun orderBooks(libraryId: Long, ids: List<Long>, token: String) = Unit
}
