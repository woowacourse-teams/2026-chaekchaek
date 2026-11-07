package com.chaekchaek.app.ui.archive

import com.chaekchaek.app.data.remote.*
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryBookToggleTest {
    private val book = BookDetailArgs("isbn", "isbn", title = "책")
    private val item = BookshelfBookSummary(42, "isbn", "책", "")
    private fun shelf(id: Long, saved: Boolean = false) = Bookshelf(id, "책장$id", id == 1L,
        if (saved) 1 else 0, if (saved) listOf(item) else emptyList())

    @Test fun `출발 책장의 담긴 책 재선택은 그 책장만 즉시 빼고 다른 책장을 보존한다`() = runToggleTest {
        val repo = ToggleShelves(listOf(shelf(1, true), shelf(2, true)))
        val removed = mutableListOf<RemovedShelfBook>()
        val vm = model(repo, removed::add)
        vm.toggleBookInShelf(book, 2); advanceUntilIdle()
        assertEquals(listOf(2L to 42L), repo.removed)
        assertTrue(repo.libraries.first().items.isNotEmpty())
        assertTrue(repo.added.isEmpty())
        assertNull(vm.uiState.value.choosingBook)
        assertEquals(true, removed.single().savedElsewhere)
        assertEquals(1, vm.uiState.value.removedCount)
    }

    @Test fun `일반 재선택은 담긴 두 책장만 보여주고 선택한 책장 하나만 뺀다`() = runToggleTest {
        val repo = ToggleShelves(listOf(shelf(1, true), shelf(2), shelf(3, true)))
        val vm = model(repo)
        vm.toggleSavedBook(book); advanceUntilIdle()
        assertEquals(ShelfBookAction.Remove, vm.uiState.value.shelfAction)
        assertEquals(listOf(1L, 3L), vm.uiState.value.shelves.map { it.libraryId })
        assertTrue(repo.removed.isEmpty())
        vm.selectShelf(2)
        assertEquals(1L, vm.uiState.value.selectedShelfId)
        vm.selectShelf(3); vm.confirmShelf(); advanceUntilIdle()
        assertEquals(listOf(3L to 42L), repo.removed)
        assertNull(vm.uiState.value.choosingBook)
    }

    @Test fun `담긴 책장이 하나면 그 책장만 표시하고 닫으면 삭제하지 않는다`() = runToggleTest {
        val repo = ToggleShelves(listOf(shelf(1), shelf(2, true), shelf(3)))
        val vm = model(repo)
        vm.toggleSavedBook(book); advanceUntilIdle()
        assertEquals(listOf(2L), vm.uiState.value.shelves.map { it.libraryId })
        assertEquals(2L, vm.uiState.value.selectedShelfId)
        vm.dismissShelfChoice(); vm.confirmShelf(); advanceUntilIdle()
        assertNull(vm.uiState.value.choosingBook)
        assertTrue(repo.removed.isEmpty())
    }

    @Test fun `없는 책의 담기는 선택 후 저장하고 다른 책장 담기 메뉴는 빼기로 바뀌지 않는다`() = runToggleTest {
        val repo = ToggleShelves(listOf(shelf(1), shelf(2)))
        val vm = model(repo)
        vm.toggleSavedBook(book); advanceUntilIdle()
        assertEquals(ShelfBookAction.Add, vm.uiState.value.shelfAction)
        assertEquals(2, vm.uiState.value.shelves.size)
        vm.confirmShelf(); advanceUntilIdle()
        vm.save(book); advanceUntilIdle()
        assertEquals(ShelfBookAction.Add, vm.uiState.value.shelfAction)
        vm.selectShelf(2); vm.confirmShelf(); advanceUntilIdle()
        assertEquals(listOf(1L, 2L), repo.added)
        assertTrue(repo.removed.isEmpty())
    }

    @Test fun `삭제 실패 재시도는 같은 책장을 유지하며 중복 입력과 이전 계정 콜백을 차단한다`() = runToggleTest {
        val repo = ToggleShelves(listOf(shelf(1, true), shelf(2)))
        val vm = model(repo)
        vm.toggleSavedBook(book); advanceUntilIdle()
        repo.removeFails = true
        vm.confirmShelf(); vm.confirmShelf(); advanceUntilIdle()
        assertEquals(listOf(1L to 42L), repo.removed)
        assertEquals(1L, vm.uiState.value.selectedShelfId)
        assertNotNull(vm.uiState.value.choosingBook)
        val retry = assertNotNull(vm.errors.takeRecovery(vm.errors.notice.value!!.id))
        repo.removeFails = false
        retry("renewed"); advanceUntilIdle()
        assertEquals(listOf("token", "renewed"), repo.removeTokens)
        assertEquals(1, vm.uiState.value.removedCount)
        vm.authenticate("other", 10)
        retry("stale"); advanceUntilIdle()
        assertEquals(2, repo.removed.size)
    }

    @Test fun `서버 삭제는 성공했지만 응답이 실패한 재시도도 담기로 뒤집지 않는다`() = runToggleTest {
        val repo = ToggleShelves(listOf(shelf(1, true)))
        val removed = mutableListOf<RemovedShelfBook>()
        val vm = model(repo, removed::add)
        repo.failAfterRemoval = true
        vm.toggleBookInShelf(book, 1); advanceUntilIdle()
        assertTrue(repo.libraries.single().items.isEmpty())
        repo.failAfterRemoval = false
        vm.errors.takeRecovery(vm.errors.notice.value!!.id)!!(null); advanceUntilIdle()
        assertEquals(listOf(1L to 42L, 1L to 42L), repo.removed)
        assertTrue(repo.added.isEmpty())
        assertFalse(removed.single().savedElsewhere)
    }

    @Test fun `출발 책장 로그인 대기는 빼기 의도를 보존하고 계정 변경은 진행 중 결과를 버린다`() = runToggleTest {
        val repo = ToggleShelves(listOf(shelf(1, true)))
        val vm = model(repo)
        vm.authenticate(null)
        vm.toggleBookInShelf(book, 1)
        assertTrue(vm.uiState.value.pendingToggle)
        vm.authenticate("renewed", 9); advanceUntilIdle()
        assertEquals(listOf(1L to 42L), repo.removed)
        repo.libraries = listOf(shelf(1, true))
        repo.removeGate = CompletableDeferred()
        vm.toggleBookInShelf(book, 1); runCurrent()
        vm.authenticate("other", 10)
        repo.removeGate!!.complete(Unit); advanceUntilIdle()
        assertEquals(0, vm.uiState.value.removedCount)
        assertEquals(10L, vm.uiState.value.memberId)
    }

    private fun model(repo: ToggleShelves, onRemoved: (RemovedShelfBook) -> Unit = {}) =
        LibrarySaveViewModel({ _, _, _ -> error("이전 저장 API 호출 금지") }, { _, _ -> },
            shelves = repo, onRemoved = onRemoved).also { it.authenticate("token", 9) }

    private fun runToggleTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block() } finally { Dispatchers.resetMain() }
    }
}

private class ToggleShelves(var libraries: List<Bookshelf>) : BookshelfRepository {
    val added = mutableListOf<Long>()
    val removed = mutableListOf<Pair<Long, Long>>()
    val removeTokens = mutableListOf<String>()
    var removeFails = false
    var failAfterRemoval = false
    var removeGate: CompletableDeferred<Unit>? = null
    override suspend fun mine(token: String) = BookshelfCollection(9, libraries)
    override suspend fun add(libraryId: Long, isbn13: String, totalPages: Int?, token: String): LibraryRecord {
        added += libraryId
        libraries = libraries.map { if (it.libraryId == libraryId) it.copy(items = it.items + BookshelfBookSummary(42, isbn13, "책", ""), totalCount = it.totalCount + 1) else it }
        return LibraryRecord("READING", 0, null, 42)
    }
    override suspend fun remove(libraryId: Long, bookId: Long, token: String) {
        removed += libraryId to bookId
        removeTokens += token
        removeGate?.await()
        check(!removeFails) { "삭제 실패" }
        libraries = libraries.map { if (it.libraryId == libraryId) it.copy(items = it.items.filterNot { it.bookId == bookId }) else it }
        check(!failAfterRemoval) { "응답 실패" }
    }
    override suspend fun detail(libraryId: Long): BookshelfDetail = error("사용하지 않음")
    override suspend fun create(name: String, token: String): Bookshelf = error("사용하지 않음")
    override suspend fun rename(libraryId: Long, name: String, token: String) = Unit
    override suspend fun delete(libraryId: Long, token: String) = Unit
    override suspend fun orderShelves(ids: List<Long>, token: String) = Unit
    override suspend fun orderBooks(libraryId: Long, ids: List<Long>, token: String) = Unit
}
