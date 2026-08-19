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
      myRecord = LibraryRecordDto(status = "READING", currentPage = 120, rating = 4.5),
    ).toBookDetail()
    val reviews = ReviewPageDto(
      totalCount = 1,
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
    assertEquals("참새 1204", reviews.items.single().authorName)
  }
}
