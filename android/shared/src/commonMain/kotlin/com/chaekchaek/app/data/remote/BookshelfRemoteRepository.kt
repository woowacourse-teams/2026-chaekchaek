package com.chaekchaek.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

interface BookshelfRepository {
    suspend fun mine(token: String): BookshelfCollection
    suspend fun detail(libraryId: Long): BookshelfDetail
    suspend fun create(name: String, token: String): Bookshelf
    suspend fun rename(libraryId: Long, name: String, token: String)
    suspend fun delete(libraryId: Long, token: String)
    suspend fun add(libraryId: Long, isbn13: String, totalPages: Int?, token: String): LibraryRecord
    suspend fun remove(libraryId: Long, bookId: Long, token: String)
    suspend fun orderShelves(ids: List<Long>, token: String)
    suspend fun orderBooks(libraryId: Long, ids: List<Long>, token: String)
}

/** 독서 기록과 별개인 책장 및 도서 배치를 소유한다. */
class BookshelfRemoteRepository(
    private val client: HttpClient = createHttpClient(),
    private val configuration: ApiConfiguration = ApiConfiguration.current,
) : BookshelfRepository {
    override suspend fun mine(token: String): BookshelfCollection {
        val member = MemberRemoteRepository(client, configuration).get(token)
        return collection(member.memberId)
    }

    suspend fun collection(memberId: Long): BookshelfCollection = client.get(url("/api/v1/libraries")) {
        parameter("memberId", memberId)
    }.body()

    override suspend fun detail(libraryId: Long): BookshelfDetail =
        client.get(url("/api/v1/libraries/$libraryId")).body()

    override suspend fun create(name: String, token: String): Bookshelf = client.post(url("/api/v1/libraries")) {
        authenticated(token, BookshelfNameRequest(name))
    }.body()

    override suspend fun rename(libraryId: Long, name: String, token: String) {
        client.patch(url("/api/v1/libraries/$libraryId")) { authenticated(token, BookshelfNameRequest(name)) }
    }

    override suspend fun delete(libraryId: Long, token: String) {
        client.delete(url("/api/v1/libraries/$libraryId")) { bearerAuth(token) }
    }

    override suspend fun add(libraryId: Long, isbn13: String, totalPages: Int?, token: String): LibraryRecord {
        val record = client.post(url("/api/v1/libraries/$libraryId")) {
            authenticated(token, BookshelfBookRequest(isbn13, totalPages, "READING"))
        }.body<BookshelfSavedRecord>()
        return LibraryRecord(record.status.orEmpty(), record.currentPage ?: 0, record.rating, record.bookId)
    }

    override suspend fun remove(libraryId: Long, bookId: Long, token: String) {
        client.delete(url("/api/v1/libraries/$libraryId/books/$bookId")) { bearerAuth(token) }
    }

    override suspend fun orderShelves(ids: List<Long>, token: String) {
        client.patch(url("/api/v1/libraries/order")) { authenticated(token, BookshelfOrderRequest(ids)) }
    }

    override suspend fun orderBooks(libraryId: Long, ids: List<Long>, token: String) {
        client.patch(url("/api/v1/libraries/$libraryId/books/order")) { authenticated(token, BookshelfBookOrderRequest(ids)) }
    }

    private fun url(path: String) = configuration.baseUrl + path
    private inline fun <reified T> HttpRequestBuilder.authenticated(token: String, body: T) {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(body)
    }
}

@Serializable
data class BookshelfCollection(val memberId: Long, val libraries: List<Bookshelf>)

@Serializable
data class Bookshelf(
    val libraryId: Long,
    val name: String,
    val isDefault: Boolean,
    val totalCount: Int,
    val items: List<BookshelfBookSummary>,
)

@Serializable
data class BookshelfBookSummary(
    val bookId: Long,
    val isbn13: String,
    val title: String,
    val spineImageUrl: String? = null,
)

@Serializable
data class BookshelfDetail(
    val libraryId: Long,
    val memberId: Long,
    val name: String,
    val isDefault: Boolean,
    val totalCount: Int,
    val items: List<BookshelfBook>,
)

@Serializable
data class BookshelfBook(
    val bookId: Long,
    val isbn13: String,
    val title: String,
    val coverImageUrl: String,
    val spineImageUrl: String? = null,
    val authors: List<String> = emptyList(),
    val currentPage: Int = 0,
    val totalPages: Int? = null,
    val status: String? = null,
    val readingUpdatedAt: String? = null,
)

@Serializable private data class BookshelfNameRequest(val name: String)
@Serializable private data class BookshelfBookRequest(val isbn13: String, val totalPages: Int?, val status: String)
@Serializable private data class BookshelfOrderRequest(val libraryIds: List<Long>)
@Serializable private data class BookshelfBookOrderRequest(val bookIds: List<Long>)
@Serializable private data class BookshelfSavedRecord(
    val bookId: Long,
    val status: String? = null,
    val currentPage: Int? = null,
    val rating: Double? = null,
)
