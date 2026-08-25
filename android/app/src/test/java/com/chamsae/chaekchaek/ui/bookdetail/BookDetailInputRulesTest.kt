package com.chamsae.chaekchaek.ui.bookdetail

import com.chamsae.chaekchaek.R
import com.chaekchaek.app.data.remote.ReplyPage
import com.chaekchaek.app.data.remote.ReviewReply
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookDetailInputRulesTest {
  @Test
  fun `감상은 내용이 있고 쪽수가 전체 범위 안일 때 제출할 수 있다`() {
    assertTrue(BookDetailInputRules.canSubmitReview("좋았다", "80", 308))
    assertTrue(BookDetailInputRules.canSubmitReview("좋았다", "", 308))
    assertFalse(BookDetailInputRules.canSubmitReview("  ", "80", 308))
    assertFalse(BookDetailInputRules.canSubmitReview("좋았다", "309", 308))
  }

  @Test
  fun `기본 쪽수 외에 작성값이 생기면 임시 감상으로 판단한다`() {
    assertFalse(BookDetailInputRules.hasReviewDraft("", "", "", "80", 80, false))
    assertTrue(BookDetailInputRules.hasReviewDraft("한 글자", "", "", "80", 80, false))
    assertTrue(BookDetailInputRules.hasReviewDraft("", "", "", "81", 80, false))
    assertTrue(BookDetailInputRules.hasReviewDraft("", "", "", "80", 80, true))
  }

  @Test
  fun `전체 답글은 마지막 페이지까지 읽고 중복을 제거한다`() = runTest {
    val requestedPages = mutableListOf<Int>()
    val first = ReviewReply(1, "첫 답글", "참새", false, 0)
    val second = ReviewReply(2, "둘째 답글", "참새", false, 0)

    val replies = loadAllReplies { page ->
      requestedPages += page
      if (page == 1) ReplyPage(2, 2, listOf(first))
      else ReplyPage(2, null, listOf(first, second))
    }

    assertEquals(listOf(1, 2), requestedPages)
    assertEquals(listOf(first, second), replies)
  }

  @Test
  fun `감상 쪽수가 읽은 범위를 넘고 전체 공개하지 않았을 때만 잠근다`() {
    assertFalse(shouldLockReview(currentPage = 80, reviewPage = 80, spoilersRevealed = false))
    assertTrue(shouldLockReview(currentPage = 80, reviewPage = 160, spoilersRevealed = false))
    assertFalse(shouldLockReview(currentPage = 80, reviewPage = null, spoilersRevealed = false))
    assertFalse(shouldLockReview(currentPage = 80, reviewPage = 160, spoilersRevealed = true))
  }

  @Test
  fun `잠긴 텍스트는 원문 길이와 공백과 문장부호를 유지한다`() {
    val content = "감상 42쪽!\n좋다?"

    val masked = maskAsChirps(content)

    assertEquals(content.length, masked.length)
    assertEquals("짹짹 짹짹짹!\n짹짹?", masked)
  }

  @Test
  fun `같은 표시 이름은 항상 같은 참새 프로필을 사용한다`() {
    val first = authorAvatarResource("참새 1204")
    val second = authorAvatarResource("참새 1204")

    assertEquals(first, second)
    assertTrue(
      first in setOf(
        R.drawable.avatar_reading,
        R.drawable.avatar_kim,
        R.drawable.avatar_yoon,
        R.drawable.avatar_tea,
      ),
    )
  }
}
