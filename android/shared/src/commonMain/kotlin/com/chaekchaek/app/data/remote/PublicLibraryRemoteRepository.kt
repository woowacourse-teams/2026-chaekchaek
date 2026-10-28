package com.chaekchaek.app.data.remote

import com.chaekchaek.app.domain.library.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

/** 공개 조회에는 로그인 토큰이나 게스트 쓰기 토큰을 보내지 않는다. */
class PublicLibraryRemoteRepository(
    private val client: HttpClient = createHttpClient(),
    private val apiConfiguration: ApiConfiguration = ApiConfiguration.current,
) : PublicLibraryRepository {
    override suspend fun library(memberId: Long): PublicLibrary {
        require(memberId > 0)
        val response = client.get("${apiConfiguration.baseUrl}/api/v1/libraries") {
            parameter("memberId", memberId)
        }.body<PublicLibraryDto>()
        check(response.memberId == memberId) { "서재 소유자가 일치하지 않아요" }
        return PublicLibrary(response.memberId, response.libraries.map { shelf ->
            PublicBookshelf(shelf.libraryId, shelf.name, shelf.isDefault, shelf.totalCount,
                shelf.items.map { PublicSpineBook(it.bookId, it.isbn13, it.title, it.spineImageUrl, it.coverImageUrl, it.authors) })
        })
    }
    override suspend fun bookshelf(libraryId: Long): PublicBookshelfDetail {
        require(libraryId > 0)
        val response = client.get("${apiConfiguration.baseUrl}/api/v1/libraries/$libraryId").body<PublicBookshelfDto>()
        check(response.libraryId == libraryId) { "책장이 일치하지 않아요" }
        return PublicBookshelfDetail(response.libraryId, response.memberId, response.name, response.isDefault,
            response.totalCount, response.items.map { book ->
                PublicLibraryBook(book.bookId, book.isbn13, book.title, book.coverImageUrl, book.spineImageUrl,
                    book.authors, book.totalPages, book.status, book.currentPage)
            })
    }
}
@Serializable
private data class PublicLibraryDto(val memberId: Long, val libraries: List<PublicShelfSummaryDto>)
@Serializable
private data class PublicShelfSummaryDto(
    val libraryId: Long, val name: String, val isDefault: Boolean, val totalCount: Long,
    val items: List<PublicSpineDto>,
)
@Serializable
private data class PublicSpineDto(val bookId: Long, val isbn13: String, val title: String, val coverImageUrl: String, val spineImageUrl: String? = null, val authors: List<String> = emptyList())
@Serializable
private data class PublicBookshelfDto(
    val libraryId: Long, val memberId: Long, val name: String, val isDefault: Boolean,
    val totalCount: Long, val items: List<PublicLibraryBookDto>,
)
@Serializable
private data class PublicLibraryBookDto(
    val bookId: Long, val isbn13: String, val title: String, val coverImageUrl: String,
    val spineImageUrl: String? = null, val authors: List<String>, val totalPages: Int? = null,
    val status: String, val currentPage: Int,
)
