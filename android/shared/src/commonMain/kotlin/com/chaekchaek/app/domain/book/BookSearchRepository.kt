package com.chaekchaek.app.domain.book

fun interface BookSearchRepository {
    suspend fun search(query: String, sort: BookSearchSort, page: Int): BookSearchPage

    // 저장된 범위를 재조회할 때는 빈 페이지도 다음 서버 페이지로 넘어가지 않고 반환한다.
    suspend fun searchPage(query: String, sort: BookSearchSort, page: Int): BookSearchPage =
        search(query, sort, page)
}

enum class BookSearchSort { LATEST, COMMENT }

data class BookSearchPage(
    val totalCount: Int,
    val nextPage: Int?,
    val items: List<BookSearchResult>,
    val lastLoadedPage: Int? = null,
)

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
