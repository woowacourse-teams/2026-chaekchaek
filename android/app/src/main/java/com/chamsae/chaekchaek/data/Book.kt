package com.chamsae.chaekchaek.data

import org.json.JSONArray
import org.json.JSONObject

data class BookSearchResult(
  val title: String,
  val creator: String,
  val publisher: String,
  val year: String,
  val coverUrl: String,
  val description: String,
  val isbn13: String = "",
)

data class ArchivedBook(
  val id: String,
  val title: String,
  val creator: String,
  val publisher: String,
  val year: String,
  val coverUrl: String,
  val note: String,
)

/**
 * 알라딘 ItemSearch API(`output=js`) 응답을 파싱한다. 최상위 `item` 배열 아래 각 도서 객체의
 * `author`/`pubDate`/`cover` 필드명을 우리 모델 이름에 맞춰 옮긴다. 필드가 없으면 빈 문자열로
 * 채운다. `pubDate`는 `YYYY-MM-DD` 형식이라 앞 4자리만 연도로 사용한다.
 */
internal fun parseBookSearchResults(json: String): List<BookSearchResult> {
  val items = JSONObject(json).optJSONArray("item") ?: JSONArray()
  return List(items.length()) { i ->
    val obj = items.getJSONObject(i)
    BookSearchResult(
      title = obj.optString("title"),
      creator = obj.optString("author"),
      publisher = obj.optString("publisher"),
      year = obj.optString("pubDate").take(4),
      coverUrl = obj.optString("cover"),
      description = obj.optString("description"),
      isbn13 = obj.optString("isbn13"),
    )
  }
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

internal fun JSONObject.toArchivedBook(): ArchivedBook =
  ArchivedBook(
    id = getString("id"),
    title = optString("title"),
    creator = optString("creator"),
    publisher = optString("publisher"),
    year = optString("year"),
    coverUrl = optString("coverUrl"),
    note = optString("note"),
  )

internal fun parseArchivedBooks(json: String): List<ArchivedBook> {
  val array = JSONArray(json)
  return List(array.length()) { i -> array.getJSONObject(i).toArchivedBook() }
}

internal fun serializeArchivedBooks(items: List<ArchivedBook>): String {
  val array = JSONArray()
  items.forEach { array.put(it.toJson()) }
  return array.toString()
}
