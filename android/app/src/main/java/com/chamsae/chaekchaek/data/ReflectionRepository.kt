package com.chamsae.chaekchaek.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ReflectionRepository {
  val reflections: StateFlow<List<BookReflection>>
  val replies: StateFlow<List<ReflectionReply>>

  fun add(reflection: BookReflection)

  fun addReply(reply: ReflectionReply)
}

/**
 * 감상을 기기 로컬에 저장한다.
 *
 * ponytail: 감상마다 JSON 배열 전체를 저장한다. 수백 건 이상이거나 조회 조건이 늘면 Room으로 전환한다.
 */
class PreferencesReflectionRepository(context: Context) : ReflectionRepository {
  private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val _reflections = MutableStateFlow(loadAll())
  private val _replies = MutableStateFlow(loadReplies())
  override val reflections: StateFlow<List<BookReflection>> = _reflections.asStateFlow()
  override val replies: StateFlow<List<ReflectionReply>> = _replies.asStateFlow()

  override fun add(reflection: BookReflection) {
    val updated = listOf(reflection) + _reflections.value
    _reflections.value = updated
    prefs.edit().putString(KEY_ITEMS, serializeBookReflections(updated)).apply()
  }

  override fun addReply(reply: ReflectionReply) {
    val updated = listOf(reply) + _replies.value
    _replies.value = updated
    prefs.edit().putString(KEY_REPLIES, serializeReflectionReplies(updated)).apply()
  }

  private fun loadAll(): List<BookReflection> {
    val json = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
    return parseBookReflections(json)
  }

  private fun loadReplies(): List<ReflectionReply> {
    val json = prefs.getString(KEY_REPLIES, null) ?: return emptyList()
    return parseReflectionReplies(json)
  }

  private companion object {
    const val PREFS_NAME = "reflections"
    const val KEY_ITEMS = "items"
    const val KEY_REPLIES = "replies"
  }
}
