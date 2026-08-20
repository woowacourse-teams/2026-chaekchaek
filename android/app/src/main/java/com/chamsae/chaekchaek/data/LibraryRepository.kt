package com.chamsae.chaekchaek.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface LibraryRepository {
  val items: StateFlow<List<ArchivedBook>>
  val anonymousReviews: StateFlow<Boolean>
  val nickname: StateFlow<String>

  fun add(book: ArchivedBook)

  fun remove(bookIds: Set<String>)

  fun changeStatus(bookIds: Set<String>, status: ReadingStatus)

  fun setAnonymousReviews(anonymous: Boolean, nickname: String = "")
}

/**
 * 등록된 책을 기기 로컬에만 저장한다(로그인 없음, 샘플앱 범위).
 *
 * ponytail: SharedPreferences에 JSON 배열 통째로 저장 - 등록마다 전체 목록을 다시 쓴다(O(n)).
 * 아카이브가 수백 건 이상으로 커지거나 오프라인 쿼리가 필요해지면 Room으로 전환.
 */
class PreferencesLibraryRepository(
  context: Context,
  private val accessToken: () -> String?,
  private val addToRemoteLibrary: suspend (String, Int?, String) -> Unit,
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : LibraryRepository {
  private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val pendingBookIds = mutableSetOf<String>()

  private val _items = MutableStateFlow(loadAll())
  override val items: StateFlow<List<ArchivedBook>> = _items.asStateFlow()
  private val _anonymousReviews = MutableStateFlow(prefs.getBoolean(KEY_ANONYMOUS, true))
  override val anonymousReviews: StateFlow<Boolean> = _anonymousReviews.asStateFlow()
  private val _nickname = MutableStateFlow(prefs.getString(KEY_NICKNAME, "").orEmpty())
  override val nickname: StateFlow<String> = _nickname.asStateFlow()

  override fun add(book: ArchivedBook) {
    if (_items.value.any { it.id == book.id } || !pendingBookIds.add(book.id)) return
    val token = accessToken()
    if (token == null) {
      pendingBookIds -= book.id
      return
    }
    scope.launch {
      try {
        if (registerRemotely(book, token, addToRemoteLibrary)) {
          save(_items.value.plus(book.copy(lastRecordedAt = System.currentTimeMillis())))
        }
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        // 원격 등록이 실패하면 로컬 상태도 등록하지 않는다.
      } finally {
        pendingBookIds -= book.id
      }
    }
  }

  override fun remove(bookIds: Set<String>) {
    save(_items.value.filterNot { it.id in bookIds })
  }

  override fun changeStatus(bookIds: Set<String>, status: ReadingStatus) {
    val recordedAt = System.currentTimeMillis()
    save(_items.value.map { if (it.id in bookIds) it.changedTo(status, recordedAt) else it })
  }

  override fun setAnonymousReviews(anonymous: Boolean, nickname: String) {
    val trimmedNickname = nickname.trim()
    _anonymousReviews.value = anonymous
    _nickname.value = trimmedNickname
    prefs.edit()
      .putBoolean(KEY_ANONYMOUS, anonymous)
      .putString(KEY_NICKNAME, trimmedNickname)
      .apply()
  }

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

internal suspend fun registerRemotely(
  book: ArchivedBook,
  accessToken: String,
  addToRemoteLibrary: suspend (String, Int?, String) -> Unit,
): Boolean {
  if (book.id.length != ISBN13_LENGTH || !book.id.all(Char::isDigit)) return false
  addToRemoteLibrary(book.id, book.totalPages.takeIf { it > 0 }, accessToken)
  return true
}

private const val ISBN13_LENGTH = 13
