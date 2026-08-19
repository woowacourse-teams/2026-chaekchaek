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
  fun savedRatingSurvivesStoreRecreation() {
    BookRatingStore(context).rate("book-test", "테스트 책", Rating.ofHalfStars(7))

    val restored = BookRatingStore(context).ratings.value.first { it.bookId == "book-test" }

    assertEquals(7, restored.rating.halfStars)
    assertEquals("테스트 책", restored.title)
  }
}
