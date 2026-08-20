package com.chamsae.chaekchaek.data

import com.chaekchaek.app.domain.book.BookSearchResult

enum class ReadingStatus(val label: String) {
  WantToRead("읽고 싶어요"),
  Reading("읽는 중"),
  Finished("다 읽음"),
}

internal val ReadingStatus.apiValue: String
  get() =
    when (this) {
      ReadingStatus.WantToRead -> "WANT_TO_READ"
      ReadingStatus.Reading -> "READING"
      ReadingStatus.Finished -> "FINISHED"
    }

data class ArchivedBook(
  val id: String,
  val bookId: Long? = null,
  val title: String,
  val creator: String,
  val publisher: String,
  val year: String,
  val coverUrl: String,
  val note: String,
  val category: String = "",
  val status: ReadingStatus = ReadingStatus.Reading,
  val currentPage: Int = 0,
  val totalPages: Int = 0,
  val lastRecordedAt: Long = 0L,
) {
  init {
    require(currentPage >= 0) { "현재 쪽수는 0보다 작을 수 없습니다." }
    require(totalPages >= 0) { "전체 쪽수는 0보다 작을 수 없습니다." }
    require(totalPages == 0 || currentPage <= totalPages) { "현재 쪽수는 전체 쪽수를 넘을 수 없습니다." }
  }

  val progressRatio: Float
    get() = if (totalPages == 0) 0f else currentPage.toFloat() / totalPages

  fun changedTo(next: ReadingStatus, recordedAt: Long): ArchivedBook =
    copy(
      status = next,
      currentPage =
        when (next) {
          ReadingStatus.WantToRead -> 0
          ReadingStatus.Reading -> currentPage
          ReadingStatus.Finished -> totalPages.takeIf { it > 0 } ?: currentPage
        },
      lastRecordedAt = recordedAt,
    )
}

internal fun BookSearchResult.toArchivedBook(): ArchivedBook =
  ArchivedBook(
    id = isbn13.ifBlank { listOf(title, creator, publisher, year).joinToString("|") },
    title = title,
    creator = creator,
    publisher = publisher,
    year = year,
    coverUrl = coverUrl,
    note = "",
    category = category,
    totalPages = totalPages,
  )

internal fun List<ArchivedBook>.plusIfAbsent(book: ArchivedBook): List<ArchivedBook> =
  if (any { it.id == book.id }) this else this + book
