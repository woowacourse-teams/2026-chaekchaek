package com.chaekchaek.app.domain.reader

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private fun profile(
    nickname: Nickname? = Nickname("골똘한참새"),
    publishesAnonymously: Boolean = true,
) = ReaderProfile(ReaderId("rd_77"), nickname, publishesAnonymously)

class ReaderProfileTest {
    @Test
    fun `닉네임 없이 실명을 공개하는 프로필은 만들 수 없다`() {
        // given & when & then : Figma 의 `해제하면 닉네임을 설정해야 합니다` 가 규칙이다
        shouldThrow<IllegalArgumentException> {
            profile(nickname = null, publishesAnonymously = false)
        }
    }

    @Test
    fun `닉네임 없이 익명으로 공개할 수는 있다`() {
        // given & when : 아직 닉네임을 정하지 않은 사람이 익명으로 쓴다
        val anonymous = profile(nickname = null, publishesAnonymously = true)

        // then : 유효하고, 바로 실명으로 바꿀 수는 없다
        anonymous.canRevealName() shouldBe false
    }

    @Test
    fun `익명으로 전환해도 닉네임은 남는다`() {
        // given : 실명으로 공개하던 사람이 주어진다
        val named = profile(publishesAnonymously = false)

        // when : 익명으로 돌렸다가 다시 실명으로 바꾸면
        val hidden = named.hideName()
        val revealed = hidden.revealName()

        // then : 닉네임을 다시 입력하지 않아도 된다
        hidden.nickname shouldBe Nickname("골똘한참새")
        revealed.nickname shouldBe Nickname("골똘한참새")
    }

    @Test
    fun `닉네임을 정하면 실명으로 바꿀 수 있게 된다`() {
        // given : 닉네임이 없는 사람이 주어진다
        val anonymous = profile(nickname = null)

        // when : 닉네임을 정하면
        val named = anonymous.changeNickname(Nickname("느긋한참새"))

        // then : 실명 공개가 가능해진다
        named.canRevealName() shouldBe true
    }
}
