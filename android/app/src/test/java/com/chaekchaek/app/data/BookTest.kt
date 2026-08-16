package com.chaekchaek.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BookTest {

  @Test
  fun `parseBookSearchResults maps aladin item fields`() {
    val json =
      """
      {"version":"20131101","item":[
        {"title":"해리 포터와 마법사의 돌","author":"조앤 K. 롤링 (지은이)","pubDate":"2019-11-01","isbn13":"9788983927620",
         "description":"설명","cover":"https://image.aladin.co.kr/cover.jpg","publisher":"문학수첩"}
      ]}
      """.trimIndent()

    val results = parseBookSearchResults(json)

    assertEquals(1, results.size)
    assertEquals(
      BookSearchResult(
        title = "해리 포터와 마법사의 돌",
        creator = "조앤 K. 롤링 (지은이)",
        publisher = "문학수첩",
        year = "2019",
        coverUrl = "https://image.aladin.co.kr/cover.jpg",
        description = "설명",
        isbn13 = "9788983927620",
      ),
      results.first(),
    )
  }

  @Test
  fun `parseBookSearchResults returns empty list when item is missing`() {
    assertEquals(emptyList<BookSearchResult>(), parseBookSearchResults("""{"version":"20131101"}"""))
  }

  @Test
  fun `archived books round-trip through json`() {
    val original =
      listOf(
        ArchivedBook(
          id = "1",
          title = "제목",
          creator = "저자",
          publisher = "출판사",
          year = "2024",
          coverUrl = "https://example.com/cover.jpg",
          note = "한줄평",
        )
      )

    val restored = parseArchivedBooks(serializeArchivedBooks(original))

    assertEquals(original, restored)
  }

  @Test
  fun `search result converts to one unique archived book`() {
    val archived =
      BookSearchResult(
        title = "마션",
        creator = "앤디 위어",
        publisher = "알에이치코리아",
        year = "2026",
        coverUrl = "https://example.com/martian.jpg",
        description = "",
        isbn13 = "9788925588650",
      ).toArchivedBook()

    val items = emptyList<ArchivedBook>().plusIfAbsent(archived).plusIfAbsent(archived)

    assertEquals("9788925588650", archived.id)
    assertEquals(listOf(archived), items)
  }
}
