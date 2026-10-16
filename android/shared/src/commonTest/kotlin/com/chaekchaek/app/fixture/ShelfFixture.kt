package com.chaekchaek.app.fixture

import com.chaekchaek.app.domain.book.Book
import com.chaekchaek.app.domain.book.BookId
import com.chaekchaek.app.domain.book.PageCount
import com.chaekchaek.app.domain.book.PageNumber
import com.chaekchaek.app.domain.rating.Rating
import com.chaekchaek.app.domain.shelf.ReadingProgress
import com.chaekchaek.app.domain.shelf.ReadingStatus
import com.chaekchaek.app.domain.shelf.ShelfBook
import kotlin.time.Instant

/** 테스트에서 시각을 비교할 수 있도록 고정한 값. */
val FIXED_INSTANT: Instant = Instant.parse("2026-08-05T09:00:00Z")
val LATER_INSTANT: Instant = Instant.parse("2026-08-06T09:00:00Z")

fun book(
    id: String = "bk_003",
    title: String = "마션",
    totalPages: Int = 308,
): Book = Book(
    id = BookId(id),
    title = title,
    authors = listOf("앤디 위어"),
    translators = listOf("박아람"),
    publisher = "알에이치코리아",
    category = "SF",
    publishedYear = 2026,
    totalPages = PageCount(totalPages),
    coverId = "cover-01",
)

fun shelfBook(
    book: Book = book(),
    status: ReadingStatus = ReadingStatus.READING,
    currentPage: Int = 80,
    myRating: Rating? = null,
    at: Instant = FIXED_INSTANT,
): ShelfBook = ShelfBook(
    book = book,
    status = status,
    progress = ReadingProgress(PageNumber(currentPage), book.totalPages),
    myRating = myRating,
    lastRecordedAt = at,
)
