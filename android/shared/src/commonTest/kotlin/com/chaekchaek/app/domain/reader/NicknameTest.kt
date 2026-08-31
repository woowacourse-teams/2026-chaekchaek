package com.chaekchaek.app.domain.reader

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class NicknameTest {
    @Test
    fun `한글 영문 숫자와 대시 언더바를 쓸 수 있다`() {
        // given & when & then : 허용된 문자로만 이루어진 닉네임은 만들 수 있다
        shouldNotThrowAny {
            Nickname("골똘한참새")
            Nickname("sparrow")
            Nickname("참새1204")
            Nickname("book-lover")
            Nickname("book_lover")
        }
    }

    @Test
    fun `공백이 들어가면 만들 수 없다`() {
        // given & when & then : 띄어쓰기는 허용 문자가 아니다
        shouldThrow<IllegalArgumentException> { Nickname("골똘한 참새") }
    }

    @Test
    fun `허용하지 않는 기호가 들어가면 만들 수 없다`() {
        // given & when & then : 마침표, 느낌표, 이모지는 쓸 수 없다
        shouldThrow<IllegalArgumentException> { Nickname("참새.") }
        shouldThrow<IllegalArgumentException> { Nickname("참새!") }
        shouldThrow<IllegalArgumentException> { Nickname("참새🐦") }
    }

    @Test
    fun `자음이나 모음만으로는 만들 수 없다`() {
        // given & when & then : 완성되지 않은 한글은 닉네임으로 쓰지 않는다
        shouldThrow<IllegalArgumentException> { Nickname("ㅋㅋㅋ") }
        shouldThrow<IllegalArgumentException> { Nickname("ㅏㅏ") }
    }

    @Test
    fun `2자 미만이면 만들 수 없다`() {
        // given & when & then : 한 글자 닉네임은 너무 짧다
        shouldThrow<IllegalArgumentException> { Nickname("참") }
    }

    @Test
    fun `10자까지 만들 수 있고 11자부터는 만들 수 없다`() {
        // given : 10자와 11자 닉네임이 주어진다
        val ten = "가".repeat(10)
        val eleven = "가".repeat(11)

        // when & then : 10자는 통과하고 11자는 막힌다
        shouldNotThrowAny { Nickname(ten) }
        shouldThrow<IllegalArgumentException> { Nickname(eleven) }
    }

    @Test
    fun `빈 문자열은 만들 수 없다`() {
        // given & when & then : 아무것도 입력하지 않으면 닉네임이 아니다
        shouldThrow<IllegalArgumentException> { Nickname("") }
    }

    @Test
    fun `화면은 예외 없이 유효성을 확인할 수 있다`() {
        // given & when & then : 입력 중에도 확인 버튼 활성화를 판단할 수 있어야 한다
        Nickname.isValid("골똘한참새") shouldBe true
        Nickname.isValid("참") shouldBe false
        Nickname.isValid("골똘한 참새") shouldBe false
        Nickname.isValid("") shouldBe false
    }
}
