package com.chamsae.chaekchaek.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chaekchaek.app.domain.rating.Rating
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookRatingStoreTest {
  private val context: Context = ApplicationProvider.getApplicationContext()

  @Before
  fun clearRatings() {
    context.getSharedPreferences("book_ratings", Context.MODE_PRIVATE).edit().clear().commit()
  }

  @After
  fun cleanUp() {
    clearRatings()
  }

  @Test
  fun emptyStoreHasNoSampleRatings() {
    val store = BookRatingStore(context)
    store.selectAccount(1L)

    assertEquals(emptyList<RatedBook>(), store.ratings.value)
  }

  @Test
  fun savedRatingSurvivesStoreRecreation() {
    BookRatingStore(context).rate(1L, "book-test", "테스트 책", Rating.ofHalfStars(7))

    val restoredStore = BookRatingStore(context)
    restoredStore.selectAccount(1L)
    val restored = restoredStore.ratings.value.first { it.bookId == "book-test" }

    assertEquals(7, restored.rating.halfStars)
    assertEquals("테스트 책", restored.title)
  }

  @Test
  fun ratingsAreSeparatedByMember() {
    val store = BookRatingStore(context)
    store.rate(1L, "9780000000001", "첫 계정 책", Rating.ofHalfStars(7))
    store.rate(2L, "9780000000002", "둘째 계정 책", Rating.ofHalfStars(9))

    store.selectAccount(1L)
    assertEquals(listOf("9780000000001"), store.ratings.value.map(RatedBook::bookId))

    store.selectAccount(2L)
    assertEquals(listOf("9780000000002"), store.ratings.value.map(RatedBook::bookId))

    store.selectAccount(null)
    assertEquals(emptyList<RatedBook>(), store.ratings.value)
  }
}
