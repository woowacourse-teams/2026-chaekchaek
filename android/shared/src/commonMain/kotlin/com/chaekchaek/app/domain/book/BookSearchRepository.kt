package com.chaekchaek.app.domain.book

fun interface BookSearchRepository {
    suspend fun search(query: String, sort: BookSearchSort): List<BookSearchResult>
}

enum class BookSearchSort { LATEST, COMMENT }

data class BookSearchResult(
    val title: String,
    val creator: String,
    val publisher: String,
    val year: String,
    val coverUrl: String,
    val description: String = "",
    val isbn13: String = "",
    val category: String = "",
    val totalPages: Int = 0,
)
