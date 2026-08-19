package com.chamsae.chaekchaek.data

import com.chaekchaek.app.domain.book.BookSearchResult
import org.json.JSONArray
import org.json.JSONObject

enum class ReadingStatus(val label: String) {
  WantToRead("읽고 싶어요"),
  Reading("읽는 중"),
  Finished("다 읽음"),
}

data class ArchivedBook(
  val id: String,
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

internal fun ArchivedBook.toJson(): JSONObject =
  JSONObject()
    .put("id", id)
    .put("title", title)
    .put("creator", creator)
    .put("publisher", publisher)
    .put("year", year)
    .put("coverUrl", coverUrl)
    .put("note", note)
    .put("category", category)
    .put("status", status.name)
    .put("currentPage", currentPage)
    .put("totalPages", totalPages)
    .put("lastRecordedAt", lastRecordedAt)

internal fun JSONObject.toArchivedBook(): ArchivedBook {
  val totalPages = optInt("totalPages").coerceAtLeast(0)
  val status = ReadingStatus.entries.firstOrNull { it.name == optString("status") } ?: ReadingStatus.Reading
  val storedPage = optInt("currentPage").coerceAtLeast(0)
  val currentPage =
    when (status) {
      ReadingStatus.WantToRead -> 0
      ReadingStatus.Reading -> if (totalPages == 0) storedPage else storedPage.coerceAtMost(totalPages)
      ReadingStatus.Finished -> totalPages.takeIf { it > 0 } ?: storedPage
    }
  return ArchivedBook(
    id = getString("id"),
    title = optString("title"),
    creator = optString("creator"),
    publisher = optString("publisher"),
    year = optString("year"),
    coverUrl = optString("coverUrl"),
    note = optString("note"),
    category = optString("category"),
    status = status,
    currentPage = currentPage,
    totalPages = totalPages,
    lastRecordedAt = optLong("lastRecordedAt"),
  )
}

internal fun parseArchivedBooks(json: String): List<ArchivedBook> {
  val array = JSONArray(json)
  return List(array.length()) { i -> array.getJSONObject(i).toArchivedBook() }
}

internal fun serializeArchivedBooks(items: List<ArchivedBook>): String {
  val array = JSONArray()
  items.forEach { array.put(it.toJson()) }
  return array.toString()
}
