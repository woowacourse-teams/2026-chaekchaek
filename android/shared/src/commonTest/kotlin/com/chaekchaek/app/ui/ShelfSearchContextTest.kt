package com.chaekchaek.app.ui

import androidx.compose.runtime.saveable.SaverScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ShelfSearchContextTest {
    private val origin = ShelfSearchOrigin(42, "읽고 싶은 책", 7)
    private val context = ShelfSearchContext(origin)

    @Test fun `검색 탭을 다시 눌러도 출발 책장을 보존한다`() {
        assertEquals(origin, context.selecting(RootTab.Discover).origin)
    }

    @Test fun `다른 탭으로 이동한 뒤 검색으로 돌아오면 일반 담기다`() {
        for (tab in listOf(RootTab.Home, RootTab.Feed, RootTab.Shelf)) {
            assertNull(context.selecting(tab).selecting(RootTab.Discover).origin)
        }
    }

    @Test fun `토큰 공백과 동일 회원 재인증은 출발 책장을 유지하고 다른 회원은 해제한다`() {
        assertEquals(origin, context.authenticatedAs(7).origin)
        assertEquals(origin, context.authenticatedAs(null).origin)
        assertNull(context.authenticatedAs(8).origin)
    }

    @Test fun `명시 로그아웃은 토큰 만료와 구분하여 검색과 상세의 대상을 해제한다`() {
        assertEquals(origin, context.afterExplicitSessionReset(0).origin)
        assertNull(context.afterExplicitSessionReset(1).origin)
        assertNull(origin.inSession(1))
    }

    @Test fun `종료한 검색과 다른 책장에서 도착한 지연 담기와 복귀는 무시한다`() {
        assertTrue(context.isCurrentSearch(origin, RootTab.Discover))
        assertFalse(context.isCurrentSearch(origin, RootTab.Shelf))
        assertFalse(context.selecting(RootTab.Home).isCurrentSearch(origin, RootTab.Discover))
        val newSearch = ShelfSearchContext(ShelfSearchOrigin(99, "다음 책장", 7))
        assertFalse(newSearch.isCurrentSearch(origin, RootTab.Discover))
    }

    @Test fun `화면 복원은 출발 책장 식별자 이름과 소유자를 함께 보존한다`() {
        val scope = SaverScope { true }
        val afterLogin = ShelfSearchContext(origin.copy(sessionRevision = 3))
        val saved = with(ShelfSearchContext.Saver) { scope.save(afterLogin) }
        assertEquals(afterLogin, ShelfSearchContext.Saver.restore(saved!!))
        val empty = with(ShelfSearchContext.Saver) { scope.save(ShelfSearchContext()) }
        assertNull(empty)
        assertNull(ShelfSearchContext.Saver.restore(emptyList<Any>())!!.origin)
    }
}
