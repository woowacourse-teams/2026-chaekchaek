package com.chaekchaek.app.domain.shelf

import com.chaekchaek.app.domain.book.BookId
import com.chaekchaek.app.domain.book.PageNumber
import com.chaekchaek.app.fixture.FIXED_INSTANT
import com.chaekchaek.app.fixture.LATER_INSTANT
import com.chaekchaek.app.fixture.book
import com.chaekchaek.app.fixture.shelfBook
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.time.Instant

/** Figma 내 서재 화면(36:574)에 그려진 네 항목. */
private fun figmaShelf(): Shelf = Shelf(
    listOf(
        shelfBook(
            book = book(id = "bk_003", title = "마션", totalPages = 308),
            status = ReadingStatus.READING,
            currentPage = 80,
            at = Instant.parse("2026-08-05T09:00:00Z"),
        ),
        shelfBook(
            book = book(id = "bk_002", title = "역병", totalPages = 412),
            status = ReadingStatus.READING,
            currentPage = 210,
            at = Instant.parse("2026-08-04T09:00:00Z"),
        ),
        shelfBook(
            book = book(id = "bk_001", title = "보이지 않는 도시", totalPages = 196),
            status = ReadingStatus.FINISHED,
            currentPage = 196,
            at = Instant.parse("2026-08-03T09:00:00Z"),
        ),
        shelfBook(
            book = book(id = "bk_004", title = "침묵하는 다수", totalPages = 264),
            status = ReadingStatus.WANT_TO_READ,
            currentPage = 0,
            at = Instant.parse("2026-08-02T09:00:00Z"),
        ),
    ),
)

class ShelfFilterTest {
    @Test
    fun `상태 필터가 없으면 전체를 돌려준다`() {
        // given : 네 권이 담긴 서재가 주어진다
        val shelf = figmaShelf()

        // when & then : 필터가 null 이면 전체가 나온다
        shelf.filterBy(null).size shouldBe 4
    }

    @Test
    fun `상태로 걸러낼 수 있다`() {
        // given : 읽는 중 두 권, 다 읽음 한 권, 읽고 싶어요 한 권이 있다
        val shelf = figmaShelf()

        // when & then : 상태별로 걸러진다
        shelf.filterBy(ReadingStatus.READING).size shouldBe 2
        shelf.filterBy(ReadingStatus.FINISHED).size shouldBe 1
        shelf.filterBy(ReadingStatus.WANT_TO_READ).size shouldBe 1
    }

    @Test
    fun `전체 권수를 알려준다`() {
        // given : 네 권이 담긴 서재가 주어진다
        val shelf = figmaShelf()

        // when & then : Figma 의 `전체 N권` 표기에 쓰인다
        shelf.countOf() shouldBe 4
        shelf.countOf(ReadingStatus.READING) shouldBe 2
    }

    @Test
    fun `최근 기록순으로 정렬한다`() {
        // given : 8월 2일부터 5일까지 기록된 네 권이 있다
        val shelf = figmaShelf()

        // when : 최근 기록순으로 정렬하면
        val sorted = shelf.sortedByRecent()

        // then : 가장 최근에 기록한 마션이 맨 앞에 온다
        sorted.first().book.title shouldBe "마션"
        sorted.last().book.title shouldBe "침묵하는 다수"
    }
}

class ShelfEditTest {
    @Test
    fun `선택한 책만 상태가 바뀐다`() {
        // given : 네 권 중 마션과 역병을 골랐다
        val shelf = figmaShelf()
        val selected = setOf(BookId("bk_003"), BookId("bk_002"))

        // when : 두 권을 다 읽음으로 바꾸면
        val changed = shelf.changeStatusOf(selected, ReadingStatus.FINISHED, at = LATER_INSTANT)

        // then : 고른 두 권만 바뀌고 나머지는 그대로다
        changed.find(BookId("bk_003"))!!.status shouldBe ReadingStatus.FINISHED
        changed.find(BookId("bk_002"))!!.status shouldBe ReadingStatus.FINISHED
        changed.find(BookId("bk_004"))!!.status shouldBe ReadingStatus.WANT_TO_READ
    }

    @Test
    fun `상태를 바꾸면 쪽수도 함께 맞춰진다`() {
        // given : 308쪽 중 80쪽을 읽은 마션을 골랐다
        val shelf = figmaShelf()

        // when : 다 읽음으로 바꾸면
        val changed = shelf.changeStatusOf(
            setOf(BookId("bk_003")),
            ReadingStatus.FINISHED,
            at = LATER_INSTANT,
        )

        // then : 쪽수가 308쪽이 된다
        changed.find(BookId("bk_003"))!!.progress.currentPage shouldBe PageNumber(308)
    }

    @Test
    fun `선택한 책만 삭제된다`() {
        // given : 네 권 중 두 권을 골랐다
        val shelf = figmaShelf()

        // when : 두 권을 삭제하면
        val removed = shelf.remove(setOf(BookId("bk_003"), BookId("bk_001")))

        // then : 두 권만 남는다
        removed.countOf() shouldBe 2
        removed.contains(BookId("bk_003")) shouldBe false
        removed.contains(BookId("bk_002")) shouldBe true
    }
}

class ShelfProgressLossTest {
    @Test
    fun `읽고 싶어요로 되돌릴 때 기록을 잃는 책을 알려준다`() {
        // given : 읽는 중 두 권과 아직 안 읽은 한 권을 골랐다
        val shelf = figmaShelf()
        val selected = setOf(BookId("bk_003"), BookId("bk_002"), BookId("bk_004"))

        // when : 읽고 싶어요로 되돌리면
        val losing = shelf.booksLosingProgress(selected, ReadingStatus.WANT_TO_READ)

        // then : 읽던 두 권만 기록을 잃는다 (0쪽인 책은 잃을 것이 없다)
        losing.size shouldBe 2
        losing.map { it.book.title }.toSet() shouldBe setOf("마션", "역병")
    }

    @Test
    fun `다 읽음으로 바꿀 때는 잃는 책이 없다`() {
        // given : 네 권을 모두 골랐다
        val shelf = figmaShelf()
        val all = setOf(BookId("bk_001"), BookId("bk_002"), BookId("bk_003"), BookId("bk_004"))

        // when & then : 다 읽음은 쪽수를 지우지 않는다
        shelf.booksLosingProgress(all, ReadingStatus.FINISHED).size shouldBe 0
    }
}

class ShelfAddTest {
    @Test
    fun `서재에 없던 책을 담을 수 있다`() {
        // given : 네 권이 담긴 서재와 새 책이 주어진다
        val shelf = figmaShelf()
        val newBook = shelfBook(book = book(id = "bk_005", title = "장일장진"), currentPage = 0)

        // when : 새 책을 담으면
        val added = shelf.add(newBook)

        // then : 다섯 권이 된다
        added.countOf() shouldBe 5
        added.contains(BookId("bk_005")) shouldBe true
    }

    @Test
    fun `이미 담긴 책은 다시 담을 수 없다`() {
        // given : 마션이 이미 담긴 서재가 주어진다
        val shelf = figmaShelf()
        val duplicate = shelfBook(book = book(id = "bk_003", title = "마션"), currentPage = 0)

        // when & then : 같은 책을 또 담으려 하면 오류가 발생한다
        shouldThrow<IllegalArgumentException> { shelf.add(duplicate) }
    }
}

class EmptyShelfTest {
    @Test
    fun `빈 서재를 알아볼 수 있다`() {
        // given : 아무것도 담기지 않은 서재가 주어진다
        val shelf = Shelf(emptyList())

        // when & then : 비어 있고 권수는 0이다
        shelf.isEmpty() shouldBe true
        shelf.countOf() shouldBe 0
    }

    @Test
    fun `없는 책을 찾으면 null 을 돌려준다`() {
        // given : 네 권이 담긴 서재가 주어진다
        val shelf = figmaShelf()

        // when & then : 담기지 않은 책은 찾을 수 없다
        shelf.find(BookId("bk_999")) shouldBe null
    }
}
