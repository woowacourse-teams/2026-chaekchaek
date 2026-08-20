package com.chaekchaek.app.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals

class BookDetailRemoteRepositoryTest {
  @Test
  fun `상세와 감상 응답을 화면 모델로 변환한다`() {
    val detail = BookDetailDto(
      bookId = 42,
      isbn13 = "9788925568683",
      title = "마션",
      authors = listOf("앤디 위어"),
      translators = listOf("박아람"),
      publisher = "알에이치코리아",
      category = "국내도서>소설>과학소설",
      coverImageUrl = "https://example.com/martian.jpg",
      totalPages = 308,
      myRecord = BookDetailRecordDto(status = "READING", currentPage = 120, myRating = 4.5),
    ).toBookDetail()
    val reviews = ReviewPageDto(
      totalCount = 1,
      nextPage = 2,
      items = listOf(
        ReviewDto(
          reviewId = 7,
          content = "재미있다",
          createdAt = "2026-08-19T00:00:00Z",
          author = ReviewAuthorDto("참새 1204", true),
          replyCount = 2,
          likeCount = 3,
        ),
      ),
    ).toReviewPage()

    assertEquals("과학소설", detail.category)
    assertEquals(42, detail.bookId)
    assertEquals(120, detail.myRecord?.currentPage)
    assertEquals(4.5, detail.myRecord?.rating)
    assertEquals("참새 1204", reviews.items.single().authorName)
    assertEquals(2, reviews.nextPage)
  }

  @Test
  fun `상세 서재 기록의 비어 있는 현재 페이지는 0으로 변환한다`() {
    val record = BookDetailRecordDto(status = "WANT_TO_READ", currentPage = null, myRating = null)
      .toLibraryRecord()

    assertEquals("WANT_TO_READ", record?.status)
    assertEquals(0, record?.currentPage)
  }

  @Test
  fun `서재 변경 응답의 도서 ID를 유지한다`() {
    val record = LibraryRecordDto(bookId = 10, status = "READING", currentPage = 0)
      .toLibraryRecord()

    assertEquals(10, record.bookId)
  }
}
