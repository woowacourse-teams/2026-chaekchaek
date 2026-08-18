package com.chamsae.chaekchaek.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 등록된 책을 기기 로컬에만 저장한다(로그인 없음, 샘플앱 범위).
 *
 * ponytail: SharedPreferences에 JSON 배열 통째로 저장 - 등록마다 전체 목록을 다시 쓴다(O(n)).
 * 아카이브가 수백 건 이상으로 커지거나 오프라인 쿼리가 필요해지면 Room으로 전환.
 */
class ArchiveRepository(context: Context) {
  private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _items = MutableStateFlow(loadAll())
  val items: StateFlow<List<ArchivedBook>> = _items.asStateFlow()
  private val _anonymousReviews = MutableStateFlow(prefs.getBoolean(KEY_ANONYMOUS, true))
  val anonymousReviews: StateFlow<Boolean> = _anonymousReviews.asStateFlow()

  fun add(book: ArchivedBook) {
    val updated = _items.value.plusIfAbsent(book.copy(lastRecordedAt = System.currentTimeMillis()))
    if (updated === _items.value) return
    save(updated)
  }

  fun remove(bookIds: Set<String>) {
    save(_items.value.filterNot { it.id in bookIds })
  }

  fun changeStatus(bookIds: Set<String>, status: ReadingStatus) {
    val recordedAt = System.currentTimeMillis()
    save(_items.value.map { if (it.id in bookIds) it.changedTo(status, recordedAt) else it })
  }

  fun setAnonymousReviews(anonymous: Boolean, nickname: String = "") {
    _anonymousReviews.value = anonymous
    prefs.edit()
      .putBoolean(KEY_ANONYMOUS, anonymous)
      .putString(KEY_NICKNAME, nickname.trim())
      .apply()
  }

  fun nickname(): String = prefs.getString(KEY_NICKNAME, "").orEmpty()

  private fun save(items: List<ArchivedBook>) {
    _items.value = items
    prefs.edit().putString(KEY_ITEMS, serializeArchivedBooks(items)).apply()
  }

  private fun loadAll(): List<ArchivedBook> {
    val json = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
    return parseArchivedBooks(json)
  }

  private companion object {
    const val PREFS_NAME = "archive"
    const val KEY_ITEMS = "items"
    const val KEY_ANONYMOUS = "anonymous_reviews"
    const val KEY_NICKNAME = "nickname"
  }
}
