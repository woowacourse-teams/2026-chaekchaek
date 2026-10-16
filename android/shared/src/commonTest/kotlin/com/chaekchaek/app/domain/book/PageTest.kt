package com.chaekchaek.app.domain.book

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PageNumberTest {
    @Test
    fun `0쪽은 아직 안 읽은 상태를 뜻하므로 유효하다`() {
        // given & when & then : 0쪽을 만들면 오류가 발생하지 않는다
        shouldNotThrowAny { PageNumber(0) }
    }

    @Test
    fun `음수 쪽수는 만들 수 없다`() {
        // given & when & then : -1쪽을 만들면 오류가 발생한다
        shouldThrow<IllegalArgumentException> { PageNumber(-1) }
    }

    @Test
    fun `쪽수끼리 앞뒤를 비교할 수 있다`() {
        // given : 80쪽과 160쪽이 주어진다
        val earlier = PageNumber(80)
        val later = PageNumber(160)

        // when & then : 160쪽이 80쪽보다 뒤다
        (later > earlier) shouldBe true
    }
}

class PageCountTest {
    @Test
    fun `총 쪽수가 0이면 만들 수 없다`() {
        // given & when & then : 0쪽짜리 책은 존재하지 않는다
        shouldThrow<IllegalArgumentException> { PageCount(0) }
    }

    @Test
    fun `총 쪽수 안에 있는 지점인지 판정한다`() {
        // given : 308쪽짜리 책이 주어진다
        val total = PageCount(308)

        // when & then : 308쪽은 안에 있고 309쪽은 벗어난다
        total.contains(PageNumber(308)) shouldBe true
        total.contains(PageNumber(309)) shouldBe false
    }

    @Test
    fun `마지막 쪽을 알려준다`() {
        // given : 308쪽짜리 책이 주어진다
        val total = PageCount(308)

        // when & then : 마지막 쪽은 308쪽이다
        total.lastPage() shouldBe PageNumber(308)
    }
}
