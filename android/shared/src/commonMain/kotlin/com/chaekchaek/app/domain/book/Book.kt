package com.chaekchaek.app.domain.book

/**
 * 책 한 권. Figma 검색 결과와 책 상세에 쓰이는 서지 정보다.
 *
 * ```
 * 마션                                    title
 * 앤디 위어 · 박아람 옮김                  authors, translators
 * 알에이치코리아 · SF · 2026 · 308쪽       publisher, category, publishedYear, totalPages
 * ```
 *
 * 저자와 역자를 나눈 이유는 검색 결과가 "옮김"을 붙여 구분하고, 서재 목록은 저자만 쓰기
 * 때문이다. 표시 문자열 조합은 presentation 이 담당한다.
 */
class Book(
    val id: BookId,
    val title: String,
    val authors: List<String>,
    val translators: List<String> = emptyList(),
    val publisher: String,
    val category: String,
    val publishedYear: Int,
    val totalPages: PageCount,
    val coverId: String,
) {
    init {
        require(title.isNotBlank()) { "책 제목은 공백일 수 없습니다." }
        require(authors.isNotEmpty()) { "저자가 최소 한 명 필요합니다." }
    }
}
