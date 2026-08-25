package com.chamsae.chaekchaek.data

import android.content.Context
import com.chaekchaek.app.domain.rating.Rating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class RatedBook(
  val bookId: String,
  val title: String,
  val rating: Rating,
  val ratedAt: Long,
)

/**
 * 책별 최신 별점을 기기 로컬에 저장한다.
 *
 * ponytail: 별점 목록 전체를 JSON으로 저장한다. 수백 권 이상이 되면 Room으로 전환한다.
 */
class BookRatingStore(context: Context) {
  private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val _ratings = MutableStateFlow(emptyList<RatedBook>())
  val ratings: StateFlow<List<RatedBook>> = _ratings.asStateFlow()
  private var selectedMemberId: Long? = null

  fun selectAccount(memberId: Long?) {
    if (selectedMemberId == memberId) return
    selectedMemberId = memberId
    _ratings.value = memberId?.let(::load).orEmpty()
  }

  fun rate(memberId: Long, bookId: String, title: String, rating: Rating) {
    selectAccount(memberId)
    val updated =
      listOf(RatedBook(bookId, title, rating, System.currentTimeMillis())) +
        _ratings.value.filterNot { it.bookId == bookId }
    _ratings.value = updated
    prefs.edit().putString(key(memberId), serialize(updated)).apply()
  }

  private fun load(memberId: Long): List<RatedBook> {
    val json = prefs.getString(key(memberId), null) ?: return emptyList()
    return runCatching { parse(json) }.getOrElse { emptyList() }
  }

  private companion object {
    const val PREFS_NAME = "book_ratings"
    fun key(memberId: Long) = "items_$memberId"

    fun serialize(items: List<RatedBook>): String {
      val array = JSONArray()
      items.forEach { item ->
        array.put(
          JSONObject()
            .put("bookId", item.bookId)
            .put("title", item.title)
            .put("halfStars", item.rating.halfStars)
            .put("ratedAt", item.ratedAt),
        )
      }
      return array.toString()
    }

    fun parse(json: String): List<RatedBook> {
      val array = JSONArray(json)
      return List(array.length()) { index ->
        val item = array.getJSONObject(index)
        RatedBook(
          bookId = item.getString("bookId"),
          title = item.getString("title"),
          rating = Rating.ofHalfStars(item.getInt("halfStars")),
          ratedAt = item.getLong("ratedAt"),
        )
      }
    }
  }
}
