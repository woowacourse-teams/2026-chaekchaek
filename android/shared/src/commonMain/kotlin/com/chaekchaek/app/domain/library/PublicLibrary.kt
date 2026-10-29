package com.chaekchaek.app.domain.library

/** 항목 순서는 소유자가 정한 서버 응답 순서다. 같은 책이 서로 다른 책장에 있을 수 있다. */
data class PublicLibrary(val memberId: Long, val libraries: List<PublicBookshelf>)
data class PublicBookshelf(
    val libraryId: Long, val name: String, val isDefault: Boolean,
    val totalCount: Long, val items: List<PublicSpineBook>,
)
data class PublicSpineBook(val bookId: Long, val isbn13: String, val title: String, val spineImageUrl: String?, val coverImageUrl: String, val authors: List<String>)
data class PublicBookshelfDetail(
    val libraryId: Long, val memberId: Long, val name: String, val isDefault: Boolean,
    val totalCount: Long, val items: List<PublicLibraryBook>,
)
data class PublicLibraryBook(
    val bookId: Long, val isbn13: String, val title: String, val coverImageUrl: String,
    val spineImageUrl: String?, val authors: List<String>, val totalPages: Int?,
    val status: String, val currentPage: Int,
)
interface PublicLibraryRepository {
    suspend fun library(memberId: Long): PublicLibrary
    suspend fun bookshelf(libraryId: Long): PublicBookshelfDetail
}
