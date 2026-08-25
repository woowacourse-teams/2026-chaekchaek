package com.chaekchaek.app.data.remote

import io.ktor.client.call.body
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

class LibraryRemoteRepository(private val client: HttpClient = createHttpClient()) {
  suspend fun getMemberId(accessToken: String): Long =
    client.get("$BASE_URL/api/v1/members/me") {
      header(HttpHeaders.Authorization, "Bearer $accessToken")
    }.body<MemberResponseDto>().memberId

  suspend fun getAll(accessToken: String): List<RemoteLibraryBook> {
    val items = mutableListOf<RemoteLibraryBook>()
    var page: Int? = FIRST_PAGE
    while (page != null) {
      val response = client.get("$BASE_URL/api/v1/library") {
        header(HttpHeaders.Authorization, "Bearer $accessToken")
        parameter("page", page)
      }.body<LibraryPageDto>()
      items += response.items.map(LibraryBookDto::toRemoteLibraryBook)
      page = response.nextPage
    }
    return items
  }

  suspend fun add(isbn13: String, totalPages: Int?, accessToken: String) {
    client.post("$BASE_URL/api/v1/library") {
      authenticatedJson(accessToken, AddLibraryBookRequest(isbn13, "READING", totalPages))
    }
  }

  suspend fun bulkDelete(bookIds: List<Long>, accessToken: String) {
    chunkLibraryBookIds(bookIds).forEach { chunk ->
      client.post("$BASE_URL/api/v1/library/bulk-delete") {
        authenticatedJson(accessToken, BulkDeleteRequest(chunk))
      }
    }
  }

  suspend fun bulkChangeStatus(bookIds: List<Long>, status: String, accessToken: String) {
    chunkLibraryBookIds(bookIds).forEach { chunk ->
      client.patch("$BASE_URL/api/v1/library/bulk-status") {
        authenticatedJson(accessToken, BulkStatusRequest(chunk, status))
      }
    }
  }

  private fun io.ktor.client.request.HttpRequestBuilder.authenticatedJson(accessToken: String, body: Any) {
    header(HttpHeaders.Authorization, "Bearer $accessToken")
    contentType(ContentType.Application.Json)
    setBody(body)
  }

  private companion object {
    const val BASE_URL = "https://api.chaekchaek.com"
    const val FIRST_PAGE = 1
  }
}

internal fun chunkLibraryBookIds(bookIds: List<Long>): List<List<Long>> =
  bookIds.distinct().chunked(10)

data class RemoteLibraryBook(
  val bookId: Long,
  val isbn13: String,
  val title: String,
  val coverImageUrl: String,
  val authors: List<String>,
  val publisher: String,
  val category: String,
  val publishedDate: String,
  val totalPages: Int?,
  val status: String,
  val currentPage: Int,
  val readingUpdatedAt: String,
)

@Serializable
private data class LibraryPageDto(
  val nextPage: Int? = null,
  val items: List<LibraryBookDto>,
)

@Serializable
private data class LibraryBookDto(
  val bookId: Long,
  val isbn13: String,
  val title: String,
  val coverImageUrl: String,
  val authors: List<String>,
  val publisher: String,
  val category: String,
  val publishedDate: String,
  val totalPages: Int? = null,
  val status: String,
  val currentPage: Int,
  val readingUpdatedAt: String,
) {
  fun toRemoteLibraryBook() =
    RemoteLibraryBook(
      bookId = bookId,
      isbn13 = isbn13,
      title = title,
      coverImageUrl = coverImageUrl,
      authors = authors,
      publisher = publisher,
      category = category,
      publishedDate = publishedDate,
      totalPages = totalPages,
      status = status,
      currentPage = currentPage,
      readingUpdatedAt = readingUpdatedAt,
    )
}

@Serializable
private data class AddLibraryBookRequest(val isbn13: String, val status: String, val totalPages: Int?)

@Serializable
private data class BulkDeleteRequest(val bookIds: List<Long>)

@Serializable
private data class BulkStatusRequest(val bookIds: List<Long>, val status: String)

@Serializable
private data class MemberResponseDto(val memberId: Long)
