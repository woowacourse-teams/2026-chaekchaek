package com.chaekchaek.app.ui

import androidx.compose.runtime.saveable.listSaver
import kotlinx.serialization.Serializable

/** 검색어와 결과의 수명과 별개로, 책장 상세에서 시작한 담기의 목적지를 보관한다. */
@Serializable
internal data class ShelfSearchOrigin(
    val libraryId: Long,
    val name: String,
    val memberId: Long,
    val sessionRevision: Int = 0,
) {
    fun inSession(revision: Int): ShelfSearchOrigin? = takeIf { sessionRevision == revision }
}

/** 토큰 갱신과 일시 만료는 보존하고, 확인된 회원 변경은 열린 상세까지 무효화한다. */
internal data class ShelfSearchSession(val revision: Int = 0, val memberId: Long? = null) {
    fun authenticatedAs(currentMemberId: Long?): ShelfSearchSession = when {
        currentMemberId == null -> this
        memberId == null || memberId == currentMemberId -> copy(memberId = currentMemberId)
        else -> copy(revision = revision + 1, memberId = currentMemberId)
    }

    fun reset(): ShelfSearchSession = copy(revision = revision + 1)

    companion object {
        val Saver = listSaver<ShelfSearchSession, Any>(
            save = { listOf(it.revision, it.memberId ?: -1L) },
            restore = { ShelfSearchSession(it[0] as Int, (it[1] as Long).takeIf { id -> id != -1L }) },
        )
    }
}

internal data class ShelfSearchContext(val origin: ShelfSearchOrigin? = null) {
    fun isCurrentSearch(requestedOrigin: ShelfSearchOrigin, tab: RootTab): Boolean =
        tab == RootTab.Discover && origin == requestedOrigin

    fun selecting(tab: RootTab): ShelfSearchContext = if (tab == RootTab.Discover) this else ShelfSearchContext()

    fun authenticatedAs(memberId: Long?): ShelfSearchContext =
        if (memberId == null || origin?.memberId == memberId) this else ShelfSearchContext()

    fun afterExplicitSessionReset(revision: Int): ShelfSearchContext = ShelfSearchContext(origin?.inSession(revision))

    companion object {
        val Saver = listSaver<ShelfSearchContext, Any>(
            save = { context ->
                context.origin?.let { listOf(it.libraryId, it.name, it.memberId, it.sessionRevision) } ?: emptyList()
            },
            restore = { values ->
                ShelfSearchContext(values.takeIf { it.size == 3 || it.size == 4 }?.let {
                    ShelfSearchOrigin(it[0] as Long, it[1] as String, it[2] as Long, it.getOrNull(3) as? Int ?: 0)
                })
            },
        )
    }
}
