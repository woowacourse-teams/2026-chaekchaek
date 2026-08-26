package com.chaekchaek.app.domain.book

import kotlin.jvm.JvmInline

/**
 * 책의 어느 지점. 0쪽은 "아직 안 읽음"을 뜻하는 유효한 값이다.
 *
 * 이 앱에서 쪽수는 독서 진행과 감상 위치에 사용한다. Int 로 두면 좋아요 수 같은 다른 Int 와
 * 섞여도 컴파일이 통과하므로 전용 타입으로 감싼다.
 */
@JvmInline
value class PageNumber(val value: Int) : Comparable<PageNumber> {
    init {
        require(value >= 0) { "쪽수는 0 이상이어야 합니다: $value" }
    }

    override fun compareTo(other: PageNumber): Int = value.compareTo(other.value)

    companion object {
        val ZERO = PageNumber(0)
    }
}

/**
 * 책의 전체 분량. 0쪽짜리 책은 없으므로 [PageNumber] 와 달리 1 이상이다.
 */
@JvmInline
value class PageCount(val value: Int) {
    init {
        require(value > 0) { "총 쪽수는 1 이상이어야 합니다: $value" }
    }

    fun contains(page: PageNumber): Boolean = page.value <= value

    fun lastPage(): PageNumber = PageNumber(value)
}
