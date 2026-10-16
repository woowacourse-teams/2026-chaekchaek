package com.chaekchaek.app.domain.shelf

import com.chaekchaek.app.domain.book.PageCount
import com.chaekchaek.app.domain.book.PageNumber

/** Figma 서재 목록의 `읽고 싶어요` / `읽는 중` / `다 읽음`. */
enum class ReadingStatus {
    WANT_TO_READ,
    READING,
    FINISHED,
}

/**
 * `80쪽 / 308쪽` 처럼 어디까지 읽었는지. 총 쪽수를 넘는 진행은 만들 수 없다.
 */
class ReadingProgress(
    val currentPage: PageNumber,
    val totalPages: PageCount,
) {
    init {
        require(totalPages.contains(currentPage)) {
            "읽은 쪽수가 총 쪽수를 넘을 수 없습니다: ${currentPage.value} / ${totalPages.value}"
        }
    }

    fun isFinished(): Boolean = currentPage == totalPages.lastPage()

    fun isNotStarted(): Boolean = currentPage == PageNumber.ZERO

    fun ratio(): Float = currentPage.value.toFloat() / totalPages.value

    fun movedTo(page: PageNumber): ReadingProgress = ReadingProgress(page, totalPages)

    fun completed(): ReadingProgress = ReadingProgress(totalPages.lastPage(), totalPages)

    fun reset(): ReadingProgress = ReadingProgress(PageNumber.ZERO, totalPages)
}
