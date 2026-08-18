package com.chamsae.chaekchaek.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BookTest {

  @Test
  fun `parseBookSearchResults maps aladin item fields`() {
    val json =
      """
      {"version":"20131101","item":[
        {"title":"해리 포터와 마법사의 돌","author":"조앤 K. 롤링 (지은이)","pubDate":"2019-11-01","isbn13":"9788983927620",
         "description":"설명","cover":"https://image.aladin.co.kr/cover.jpg","publisher":"문학수첩",
         "categoryName":"국내도서>소설","subInfo":{"itemPage":308}}
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
        category = "소설",
        totalPages = 308,
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
          category = "소설",
          status = ReadingStatus.Reading,
          currentPage = 80,
          totalPages = 308,
          lastRecordedAt = 123L,
        )
      )

    val restored = parseArchivedBooks(serializeArchivedBooks(original))

    assertEquals(original, restored)
  }

  @Test
  fun `reflections round-trip through json`() {
    val original =
      listOf(
        BookReflection(
          id = "reflection-1",
          bookId = "book-1",
          body = "문제를 하나씩 푸는 태도가 인상 깊었다.",
          quote = "과학으로 살아남을 것이다.",
          page = 80,
          chapter = "Chapter 1",
          spoiler = true,
          anonymous = true,
          createdAt = 123L,
        )
      )

    assertEquals(original, parseBookReflections(serializeBookReflections(original)))
  }

  @Test
  fun `replies round-trip through json`() {
    val original =
      listOf(
        ReflectionReply(
          id = "reply-1",
          reflectionId = "reflection-1",
          body = "저도 같은 문장에서 오래 멈췄습니다.",
          anonymous = false,
          authorName = "다정한 참새",
          createdAt = 123L,
        )
      )

    assertEquals(original, parseReflectionReplies(serializeReflectionReplies(original)))
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

  @Test
  fun `status change keeps reading progress invariants`() {
    val book =
      ArchivedBook(
        id = "1",
        title = "마션",
        creator = "앤디 위어",
        publisher = "출판사",
        year = "2024",
        coverUrl = "",
        note = "",
        currentPage = 80,
        totalPages = 308,
      )

    assertEquals(0, book.changedTo(ReadingStatus.WantToRead, 1L).currentPage)
    assertEquals(80, book.changedTo(ReadingStatus.Reading, 2L).currentPage)
    assertEquals(308, book.changedTo(ReadingStatus.Finished, 3L).currentPage)
  }

  @Test
  fun `old archived book json loads with reading defaults`() {
    val restored =
      parseArchivedBooks(
        """[{"id":"1","title":"기존 책","creator":"저자","publisher":"출판사","year":"2024","coverUrl":"","note":""}]"""
      ).single()

    assertEquals(ReadingStatus.Reading, restored.status)
    assertEquals(0, restored.totalPages)
  }
}
