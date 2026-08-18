package com.chamsae.chaekchaek.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReflectionRepositoryTest {
  private val context: Context = ApplicationProvider.getApplicationContext()

  @Before
  @After
  fun clearPreferences() {
    context.getSharedPreferences("reflections", Context.MODE_PRIVATE).edit().clear().commit()
  }

  @Test
  fun replyPersistsAndUpdatesRepositoryState() {
    val reply =
      ReflectionReply(
        id = "reply-1",
        reflectionId = "sample-review-1",
        body = "저도 같은 생각이에요.",
        createdAt = 123L,
      )

    val repository = PreferencesReflectionRepository(context)
    repository.addReply(reply)

    assertEquals(listOf(reply), repository.replies.value)
    assertEquals(listOf(reply), PreferencesReflectionRepository(context).replies.value)
  }
}
