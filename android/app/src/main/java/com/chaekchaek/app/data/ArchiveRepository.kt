package com.chaekchaek.app.data

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

  fun add(book: ArchivedBook) {
    val updated = _items.value.plusIfAbsent(book)
    if (updated === _items.value) return
    _items.value = updated
    prefs.edit().putString(KEY_ITEMS, serializeArchivedBooks(updated)).apply()
  }

  private fun loadAll(): List<ArchivedBook> {
    val json = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
    return parseArchivedBooks(json)
  }

  private companion object {
    const val PREFS_NAME = "archive"
    const val KEY_ITEMS = "items"
  }
}
