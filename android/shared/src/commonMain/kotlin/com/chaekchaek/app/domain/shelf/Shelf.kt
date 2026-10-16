package com.chaekchaek.app.domain.shelf

import com.chaekchaek.app.domain.book.BookId
import kotlin.time.Instant

/**
 * 내 서재. 책 목록과 그 위에서 일어나는 규칙을 함께 갖는다.
 *
 * [books] 를 private 으로 둔 것이 핵심이다. 화면은 [filterBy]·[sortedByRecent]·[countOf] 로
 * 질문하고 내부 리스트를 직접 순회하지 않는다. 참고 프로젝트의 `Cart.cartContents` 가
 * public 이라 ViewModel 이 내부를 순회하게 된 문제를 피한다.
 */
class Shelf(
    private val books: List<ShelfBook>,
) {
    /** [status] 가 null 이면 Figma 의 `전체` 필터다. */
    fun filterBy(status: ReadingStatus?): List<ShelfBook> =
        if (status == null) books else books.filter { it.status == status }

    /** Figma 의 `최근 기록순`. */
    fun sortedByRecent(): List<ShelfBook> = books.sortedByDescending { it.lastRecordedAt }

    /** Figma 의 `전체 12권`. 필터가 걸려도 전체 권수를 보여줄 수 있게 인자를 받는다. */
    fun countOf(status: ReadingStatus? = null): Int = filterBy(status).size

    fun find(bookId: BookId): ShelfBook? = books.firstOrNull { it.book.id == bookId }

    fun contains(bookId: BookId): Boolean = find(bookId) != null

    fun isEmpty(): Boolean = books.isEmpty()

    /** 편집 모드의 `상태 변경`. 선택된 책만 바뀐다. */
    fun changeStatusOf(
        bookIds: Set<BookId>,
        next: ReadingStatus,
        at: Instant,
    ): Shelf = Shelf(
        books.map { if (it.book.id in bookIds) it.changeStatus(next, at) else it },
    )

    /**
     * 이 일괄 변경으로 진행 기록을 잃게 될 책들. 화면이 경고를 띄울지 판단하는 근거다.
     */
    fun booksLosingProgress(
        bookIds: Set<BookId>,
        next: ReadingStatus,
    ): List<ShelfBook> = books.filter { it.book.id in bookIds && it.losesProgressBy(next) }

    /** 편집 모드의 `서재에서 삭제`. */
    fun remove(bookIds: Set<BookId>): Shelf = Shelf(books.filter { it.book.id !in bookIds })

    /** 검색의 `읽는 중 시작`, 책 상세의 쪽수 기록으로 서재에 없던 책이 들어올 때. */
    fun add(shelfBook: ShelfBook): Shelf {
        require(!contains(shelfBook.book.id)) { "이미 서재에 있는 책입니다: ${shelfBook.book.title}" }
        return Shelf(books + shelfBook)
    }
}
