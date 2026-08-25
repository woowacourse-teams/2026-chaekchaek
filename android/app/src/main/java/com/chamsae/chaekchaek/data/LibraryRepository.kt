package com.chamsae.chaekchaek.data

import com.chamsae.chaekchaek.auth.AuthSession
import com.chaekchaek.app.data.remote.LibraryRemoteRepository
import com.chaekchaek.app.data.remote.RemoteLibraryBook
import com.chaekchaek.app.data.remote.RemoteMemberProfile
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface LibraryRepository {
  val items: StateFlow<List<ArchivedBook>>
  val memberId: StateFlow<Long?>
  val anonymousReviews: StateFlow<Boolean>
  val nickname: StateFlow<String>

  suspend fun add(book: ArchivedBook): Long?

  suspend fun remove(bookIds: Set<String>)

  fun changeStatus(bookIds: Set<String>, status: ReadingStatus)

  suspend fun setAnonymousReviews(anonymous: Boolean, nickname: String = "")
}

class ServerLibraryRepository(
  private val authSession: AuthSession,
  private val remoteRepository: LibraryRemoteRepository = LibraryRemoteRepository(),
) : LibraryRepository {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val mutationMutex = Mutex()

  private val _items = MutableStateFlow(emptyList<ArchivedBook>())
  override val items: StateFlow<List<ArchivedBook>> = _items.asStateFlow()
  private val _memberId = MutableStateFlow<Long?>(null)
  override val memberId: StateFlow<Long?> = _memberId.asStateFlow()
  private val _anonymousReviews = MutableStateFlow(true)
  override val anonymousReviews: StateFlow<Boolean> = _anonymousReviews.asStateFlow()
  private val _nickname = MutableStateFlow("")
  override val nickname: StateFlow<String> = _nickname.asStateFlow()

  init {
    scope.launch {
      authSession.tokens.collectLatest { tokens ->
        _items.value = emptyList()
        _memberId.value = null
        _anonymousReviews.value = true
        _nickname.value = ""
        if (tokens != null) loadAccountIgnoringFailure(tokens.accessToken)
      }
    }
  }

  override suspend fun add(book: ArchivedBook): Long? {
    val accessToken = authSession.tokens.value?.accessToken ?: return null
    return mutationMutex.withLock {
      remoteRepository.add(book.id, book.totalPages.takeIf { it > 0 }, accessToken)
      load(accessToken)
      if (authSession.tokens.value?.accessToken == accessToken) {
        _items.value.firstOrNull { it.id == book.id }?.bookId
      } else {
        null
      }
    }
  }

  override suspend fun remove(bookIds: Set<String>) {
    val serverBookIds = _items.value.filter { it.id in bookIds }.mapNotNull(ArchivedBook::bookId)
    if (serverBookIds.isEmpty()) return
    val accessToken = requireNotNull(authSession.tokens.value).accessToken
    mutationMutex.withLock {
      remoteRepository.bulkDelete(serverBookIds, accessToken)
      load(accessToken)
    }
  }

  override fun changeStatus(bookIds: Set<String>, status: ReadingStatus) {
    val serverBookIds = _items.value.filter { it.id in bookIds }.mapNotNull(ArchivedBook::bookId)
    if (serverBookIds.isEmpty()) return
    mutate { accessToken -> remoteRepository.bulkChangeStatus(serverBookIds, status.apiValue, accessToken) }
  }

  override suspend fun setAnonymousReviews(anonymous: Boolean, nickname: String) {
    val accessToken = requireNotNull(authSession.tokens.value).accessToken
    mutationMutex.withLock {
      val profile =
        if (anonymous) {
          remoteRepository.updateAnonymity(true, accessToken)
        } else {
          remoteRepository.updateNickname(nickname.trim(), accessToken)
          remoteRepository.updateAnonymity(false, accessToken)
        }
      if (authSession.tokens.value?.accessToken == accessToken) applyProfile(profile)
    }
  }

  private fun mutate(action: suspend (String) -> Unit) {
    val accessToken = authSession.tokens.value?.accessToken ?: return
    scope.launch {
      mutationMutex.withLock {
        try {
          action(accessToken)
          load(accessToken)
        } catch (error: CancellationException) {
          throw error
        } catch (_: Exception) {
        }
      }
    }
  }

  private suspend fun loadAccountIgnoringFailure(accessToken: String) {
    try {
      val profile = remoteRepository.getMember(accessToken)
      val loadedItems = remoteRepository.getAll(accessToken).map(RemoteLibraryBook::toArchivedBook)
      if (authSession.tokens.value?.accessToken == accessToken) {
        applyProfile(profile)
        _items.value = loadedItems
      }
    } catch (error: CancellationException) {
      throw error
    } catch (_: Exception) {
    }
  }

  private suspend fun load(accessToken: String) {
    val loaded = remoteRepository.getAll(accessToken).map(RemoteLibraryBook::toArchivedBook)
    if (authSession.tokens.value?.accessToken == accessToken) _items.value = loaded
  }

  private fun applyProfile(profile: RemoteMemberProfile) {
    _memberId.value = profile.memberId
    _anonymousReviews.value = profile.displayAnonymous
    _nickname.value = profile.nickname
  }
}

internal fun RemoteLibraryBook.toArchivedBook(): ArchivedBook {
  val pages = totalPages?.coerceAtLeast(0) ?: 0
  return ArchivedBook(
    id = isbn13,
    bookId = bookId,
    title = title,
    creator = authors.joinToString(", "),
    publisher = publisher,
    year = publishedDate.substringBefore('-'),
    coverUrl = coverImageUrl,
    note = "",
    category = category,
    status = ReadingStatus.entries.firstOrNull { it.apiValue == status } ?: ReadingStatus.Reading,
    currentPage = currentPage.coerceAtLeast(0).let { if (pages == 0) it else it.coerceAtMost(pages) },
    totalPages = pages,
    lastRecordedAt = runCatching { Instant.parse(readingUpdatedAt).toEpochMilli() }.getOrDefault(0L),
  )
}
