package com.chaekchaek.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
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

class LibraryRemoteRepository(
  private val client: HttpClient = createHttpClient(),
  private val apiConfiguration: ApiConfiguration = ApiConfiguration.current,
) {
  suspend fun getAll(accessToken: String): List<RemoteLibraryBook> {
    val items = mutableListOf<RemoteLibraryBook>()
    var page: Int? = FIRST_PAGE
    while (page != null) {
      val response = client.get("${apiConfiguration.baseUrl}/api/v1/library") {
        header(HttpHeaders.Authorization, "Bearer $accessToken")
        parameter("page", page)
      }.body<LibraryPageDto>()
      items += response.items.mapNotNull(LibraryBookDto::toRemoteLibraryBook)
      page = response.nextPage
    }
    return items
  }

  suspend fun add(isbn13: String, accessToken: String) {
    client.post("${apiConfiguration.baseUrl}/api/v1/library") {
      authenticatedJson(accessToken, AddLibraryBookRequest(isbn13))
    }
  }

  suspend fun bulkDelete(bookIds: List<Long>, accessToken: String) {
    chunkLibraryBookIds(bookIds).forEach { chunk ->
      client.post("${apiConfiguration.baseUrl}/api/v1/library/bulk-delete") {
        authenticatedJson(accessToken, BulkDeleteRequest(chunk))
      }
    }
  }

  private fun io.ktor.client.request.HttpRequestBuilder.authenticatedJson(accessToken: String, body: Any) {
    header(HttpHeaders.Authorization, "Bearer $accessToken")
    contentType(ContentType.Application.Json)
    setBody(body)
  }

  private companion object {
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
  val spineImageUrl: String?,
  val authors: List<String>,
  val publisher: String,
  val category: String,
  val publishedDate: String?,
  val readingUpdatedAt: String,
)

@Serializable
private data class LibraryPageDto(
  val nextPage: Int? = null,
  val items: List<LibraryBookDto>,
)

@Serializable
private data class LibraryBookDto(
  val bookId: Long? = null,
  val isbn13: String,
  val title: String,
  val coverImageUrl: String,
  val spineImageUrl: String? = null,
  val authors: List<String>,
  val publisher: String,
  val category: String,
  val publishedDate: String? = null,
  val readingUpdatedAt: String,
) {
  fun toRemoteLibraryBook(): RemoteLibraryBook? {
    val bookId = bookId ?: return null
    return RemoteLibraryBook(
      bookId = bookId,
      isbn13 = isbn13,
      title = title,
      coverImageUrl = coverImageUrl,
      spineImageUrl = spineImageUrl,
      authors = authors,
      publisher = publisher,
      category = category,
      publishedDate = publishedDate,
      readingUpdatedAt = readingUpdatedAt,
    )
  }
}

@Serializable
private data class AddLibraryBookRequest(val isbn13: String)

@Serializable
private data class BulkDeleteRequest(val bookIds: List<Long>)
