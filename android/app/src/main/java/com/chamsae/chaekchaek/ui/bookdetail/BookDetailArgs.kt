package com.chamsae.chaekchaek.ui.bookdetail

import com.chaekchaek.app.presentation.home.OverlappedCardUiModel
import com.chaekchaek.app.presentation.home.QuoteCardUiModel
import com.chaekchaek.app.presentation.home.ReadingBookUiModel
import com.chaekchaek.app.presentation.home.TrendingBookUiModel
import com.chaekchaek.app.domain.book.BookSearchResult
import com.chamsae.chaekchaek.data.ArchivedBook
import com.chamsae.chaekchaek.data.toArchivedBook
import kotlinx.serialization.Serializable

@Serializable
data class BookDetailArgs(
  val id: String,
  val title: String,
  val creator: String = "",
  val publisher: String = "",
  val year: String = "",
  val category: String = "",
  val totalPages: Int = 0,
  val coverUrl: String = "",
  val coverId: String = "",
)

internal fun BookSearchResult.toBookDetailArgs(): BookDetailArgs =
  BookDetailArgs(
    id = toArchivedBook().id,
    title = title,
    creator = creator,
    publisher = publisher,
    year = year,
    category = category,
    totalPages = totalPages,
    coverUrl = coverUrl,
  )

internal fun ArchivedBook.toBookDetailArgs(): BookDetailArgs =
  BookDetailArgs(
    id = id,
    title = title,
    creator = creator,
    publisher = publisher,
    year = year,
    category = category,
    totalPages = totalPages,
    coverUrl = coverUrl,
  )

internal fun TrendingBookUiModel.toBookDetailArgs(): BookDetailArgs =
  if (title == "마션") {
    BookDetailArgs(
      id = bookId.value,
      title = title,
      creator = "앤디 위어",
      publisher = "알에이치코리아",
      year = "2026",
      category = "SF",
      totalPages = 308,
      coverId = coverId,
    )
  } else {
    BookDetailArgs(id = bookId.value, title = title, coverId = coverId)
  }

internal fun QuoteCardUiModel.toBookDetailArgs(): BookDetailArgs =
  BookDetailArgs(id = bookId.value, title = bookTitle, coverId = coverId)

internal fun OverlappedCardUiModel.toBookDetailArgs(): BookDetailArgs =
  BookDetailArgs(id = bookId.value, title = title, coverId = coverId)

internal fun ReadingBookUiModel.toBookDetailArgs(): BookDetailArgs =
  BookDetailArgs(
    id = coverId,
    title = title,
    totalPages = totalPages,
    coverId = coverId,
  )
