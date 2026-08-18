package com.chaekchaek.app.domain.shelf

import com.chaekchaek.app.domain.book.PageCount
import com.chaekchaek.app.domain.book.PageNumber
import com.chaekchaek.app.fixture.FIXED_INSTANT
import com.chaekchaek.app.fixture.LATER_INSTANT
import com.chaekchaek.app.fixture.book
import com.chaekchaek.app.fixture.shelfBook
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ShelfBookStatusChangeTest {
    @Test
    fun `다 읽음으로 바꾸면 진행 쪽수가 총 쪽수가 된다`() {
        // given : 308쪽 중 80쪽을 읽은 책이 서재에 있다
        val shelfBook = shelfBook(currentPage = 80)

        // when : 상태를 다 읽음으로 바꾸면
        val finished = shelfBook.changeStatus(ReadingStatus.FINISHED, at = LATER_INSTANT)

        // then : 진행 쪽수가 308쪽이 된다
        finished.progress.currentPage shouldBe PageNumber(308)
        finished.status shouldBe ReadingStatus.FINISHED
    }

    @Test
    fun `읽고 싶어요로 바꾸면 진행 쪽수가 0이 된다`() {
        // given : 308쪽 중 80쪽을 읽은 책이 서재에 있다
        val shelfBook = shelfBook(currentPage = 80)

        // when : 상태를 읽고 싶어요로 되돌리면
        val wantToRead = shelfBook.changeStatus(ReadingStatus.WANT_TO_READ, at = LATER_INSTANT)

        // then : 진행 쪽수가 0쪽이 된다 (80쪽 기록은 사라진다)
        wantToRead.progress.currentPage shouldBe PageNumber.ZERO
        wantToRead.status shouldBe ReadingStatus.WANT_TO_READ
    }

    @Test
    fun `읽는 중으로 바꾸면 진행 쪽수가 유지된다`() {
        // given : 다 읽은 책이 서재에 있다
        val finished = shelfBook(status = ReadingStatus.FINISHED, currentPage = 308)

        // when : 다시 읽는 중으로 바꾸면
        val reading = finished.changeStatus(ReadingStatus.READING, at = LATER_INSTANT)

        // then : 쪽수는 그대로 남는다 (끝까지 읽었지만 다시 읽는 중)
        reading.progress.currentPage shouldBe PageNumber(308)
        reading.status shouldBe ReadingStatus.READING
    }

    @Test
    fun `상태를 바꾸면 기록 시각이 갱신된다`() {
        // given : 8월 5일에 기록한 책이 서재에 있다
        val shelfBook = shelfBook(at = FIXED_INSTANT)

        // when : 8월 6일에 상태를 바꾸면
        val changed = shelfBook.changeStatus(ReadingStatus.FINISHED, at = LATER_INSTANT)

        // then : 기록 시각이 8월 6일이 된다
        changed.lastRecordedAt shouldBe LATER_INSTANT
    }
}

class ShelfBookProgressLossTest {
    @Test
    fun `읽던 책을 읽고 싶어요로 되돌리면 기록이 사라진다고 알려준다`() {
        // given : 80쪽까지 읽은 책이 서재에 있다
        val shelfBook = shelfBook(currentPage = 80)

        // when & then : 읽고 싶어요로 되돌리면 80쪽 기록을 잃는다
        shelfBook.losesProgressBy(ReadingStatus.WANT_TO_READ) shouldBe true
    }

    @Test
    fun `아직 안 읽은 책은 되돌려도 잃을 기록이 없다`() {
        // given : 0쪽인 책이 서재에 있다
        val shelfBook = shelfBook(status = ReadingStatus.WANT_TO_READ, currentPage = 0)

        // when & then : 잃을 기록이 없다
        shelfBook.losesProgressBy(ReadingStatus.WANT_TO_READ) shouldBe false
    }

    @Test
    fun `다 읽음이나 읽는 중으로 바꿀 때는 기록이 사라지지 않는다`() {
        // given : 80쪽까지 읽은 책이 서재에 있다
        val shelfBook = shelfBook(currentPage = 80)

        // when & then : 두 상태 모두 쪽수를 지우지 않는다
        shelfBook.losesProgressBy(ReadingStatus.FINISHED) shouldBe false
        shelfBook.losesProgressBy(ReadingStatus.READING) shouldBe false
    }
}

class ShelfBookRecordPageTest {
    @Test
    fun `마지막 쪽을 기록하면 다 읽음이 된다`() {
        // given : 308쪽 중 80쪽을 읽은 책이 서재에 있다
        val shelfBook = shelfBook(currentPage = 80)

        // when : 308쪽을 기록하면
        val recorded = shelfBook.recordPage(PageNumber(308), at = LATER_INSTANT)

        // then : 상태가 다 읽음이 된다
        recorded.status shouldBe ReadingStatus.FINISHED
    }

    @Test
    fun `중간 쪽을 기록하면 읽는 중이 된다`() {
        // given : 아직 읽지 않은 책이 서재에 있다
        val shelfBook = shelfBook(status = ReadingStatus.WANT_TO_READ, currentPage = 0)

        // when : 160쪽을 기록하면
        val recorded = shelfBook.recordPage(PageNumber(160), at = LATER_INSTANT)

        // then : 상태가 읽는 중이 되고 쪽수가 반영된다
        recorded.status shouldBe ReadingStatus.READING
        recorded.progress.currentPage shouldBe PageNumber(160)
    }

    @Test
    fun `총 쪽수를 넘는 쪽은 기록할 수 없다`() {
        // given : 308쪽짜리 책이 서재에 있다
        val shelfBook = shelfBook(currentPage = 80)

        // when & then : 309쪽을 기록하려 하면 오류가 발생한다
        shouldThrow<IllegalArgumentException> {
            shelfBook.recordPage(PageNumber(309), at = LATER_INSTANT)
        }
    }
}

class ShelfBookInvariantTest {
    @Test
    fun `다 읽음인데 진행 쪽수가 총 쪽수가 아니면 만들 수 없다`() {
        // given & when & then : 다 읽었다면서 80쪽에 멈춘 상태는 존재할 수 없다
        shouldThrow<IllegalArgumentException> {
            shelfBook(status = ReadingStatus.FINISHED, currentPage = 80)
        }
    }

    @Test
    fun `읽고 싶어요인데 진행 쪽수가 0이 아니면 만들 수 없다`() {
        // given & when & then : 아직 안 읽었다면서 80쪽을 읽은 상태는 존재할 수 없다
        shouldThrow<IllegalArgumentException> {
            shelfBook(status = ReadingStatus.WANT_TO_READ, currentPage = 80)
        }
    }

    @Test
    fun `읽는 중은 0쪽부터 총 쪽수까지 모두 가능하다`() {
        // given & when & then : 읽는 중은 어느 지점이든 유효하다
        shelfBook(status = ReadingStatus.READING, currentPage = 0)
        shelfBook(status = ReadingStatus.READING, currentPage = 308)
    }
}

class ReadingProgressTest {
    @Test
    fun `진행 쪽수가 총 쪽수를 넘으면 만들 수 없다`() {
        // given & when & then : 308쪽짜리 책을 309쪽까지 읽을 수는 없다
        shouldThrow<IllegalArgumentException> {
            ReadingProgress(PageNumber(309), PageCount(308))
        }
    }

    @Test
    fun `진행률을 알려준다`() {
        // given : 308쪽 중 154쪽을 읽었다
        val progress = ReadingProgress(PageNumber(154), PageCount(308))

        // when & then : 절반을 읽었다
        progress.ratio() shouldBe 0.5f
    }

    @Test
    fun `Figma 서재 목록의 표기와 일치한다`() {
        // given : Figma 에 그려진 네 항목이 주어진다
        val wantToRead = shelfBook(book = book(totalPages = 264), status = ReadingStatus.WANT_TO_READ, currentPage = 0)
        val reading = shelfBook(book = book(totalPages = 308), currentPage = 80)
        val finished = shelfBook(book = book(totalPages = 196), status = ReadingStatus.FINISHED, currentPage = 196)

        // when & then : 각 상태가 Figma 표기대로 만들어진다
        wantToRead.progress.isNotStarted() shouldBe true
        reading.progress.currentPage shouldBe PageNumber(80)
        finished.progress.isFinished() shouldBe true
    }
}
