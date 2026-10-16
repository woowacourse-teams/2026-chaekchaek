package com.chaekchaek.app.domain.shelf

import com.chaekchaek.app.domain.book.Book
import com.chaekchaek.app.domain.book.PageNumber
import com.chaekchaek.app.domain.rating.Rating
import kotlin.time.Instant

/**
 * 내 서재에 담긴 책 한 권. 책 정보와 내 독서 기록을 묶는다.
 *
 * **상태가 쪽수를 강제한다.** Figma 서재 목록이 `읽고 싶어요 0쪽/264쪽`,
 * `다 읽음 196쪽/196쪽` 으로 정확히 맞아떨어지는 것을 규칙으로 채택했다. 상태를 바꾸면 쪽수가
 * 따라 움직이고, 쪽수가 끝에 닿으면 상태가 따라 움직인다.
 *
 * [at] 은 이 변경이 일어난 시각이다. 도메인이 현재 시각을 직접 읽지 않아야 테스트에서 고정할
 * 수 있으므로 호출부가 넘긴다.
 */
class ShelfBook(
    val book: Book,
    val status: ReadingStatus,
    val progress: ReadingProgress,
    val myRating: Rating? = null,
    val lastRecordedAt: Instant,
) {
    init {
        when (status) {
            ReadingStatus.FINISHED -> require(progress.isFinished()) {
                "다 읽음 상태는 진행 쪽수가 총 쪽수와 같아야 합니다."
            }
            ReadingStatus.WANT_TO_READ -> require(progress.isNotStarted()) {
                "읽고 싶어요 상태는 진행 쪽수가 0이어야 합니다."
            }
            ReadingStatus.READING -> Unit
        }
    }

    /**
     * 다 읽음으로 바꾸면 쪽수가 총 쪽수로, 읽고 싶어요로 되돌리면 0으로 맞춰진다.
     * 읽는 중으로 바꿀 때는 쪽수를 그대로 둔다. "끝까지 읽었지만 다시 읽는 중"이 가능하다.
     */
    fun changeStatus(next: ReadingStatus, at: Instant): ShelfBook = when (next) {
        ReadingStatus.FINISHED -> copyWith(next, progress.completed(), at)
        ReadingStatus.WANT_TO_READ -> copyWith(next, progress.reset(), at)
        ReadingStatus.READING -> copyWith(next, progress, at)
    }

    /** 마지막 쪽을 기록하면 다 읽음이 되고, 그 외에는 읽는 중이 된다. */
    fun recordPage(page: PageNumber, at: Instant): ShelfBook {
        val moved = progress.movedTo(page)
        val next = if (moved.isFinished()) ReadingStatus.FINISHED else ReadingStatus.READING
        return copyWith(next, moved, at)
    }

    fun rate(rating: Rating, at: Instant): ShelfBook =
        ShelfBook(book, status, progress, rating, at)

    /** 사용자가 기록한 현재 독서 쪽수. */
    fun readingPoint(): PageNumber = progress.currentPage

    /**
     * 이 상태로 바꾸면 진행 기록이 사라지는가.
     *
     * 서재 편집에서 여러 권을 한 번에 되돌릴 때 경고를 띄울지 화면이 판단하는 근거다.
     * 판단은 도메인이 하고, 경고를 띄울지는 화면이 정한다.
     */
    fun losesProgressBy(next: ReadingStatus): Boolean =
        next == ReadingStatus.WANT_TO_READ && !progress.isNotStarted()

    private fun copyWith(
        status: ReadingStatus,
        progress: ReadingProgress,
        at: Instant,
    ): ShelfBook = ShelfBook(book, status, progress, myRating, at)
}
