package com.chaekchaek.app.domain.reader

import kotlin.jvm.JvmInline

/**
 * 감상과 답글에 표시되는 이름. 한글·영문·숫자와 `-`, `_` 만 쓸 수 있고 2~10자다.
 *
 * 입력 중인 값은 아직 닉네임이 아니다. 화면은 String 을 들고 있다가 확정할 때 만들며,
 * 확인 버튼 활성화 판단에는 예외를 던지지 않는 [isValid] 를 쓴다.
 */
@JvmInline
value class Nickname(val value: String) {
    init {
        require(hasValidLength(value)) {
            "닉네임은 ${MIN_LENGTH}자 이상 ${MAX_LENGTH}자 이하여야 합니다: ${value.length}자"
        }
        require(hasAllowedCharactersOnly(value)) {
            "닉네임에는 한글, 영문, 숫자와 -, _ 만 쓸 수 있습니다: $value"
        }
    }

    companion object {
        const val MIN_LENGTH = 2
        const val MAX_LENGTH = 10

        /** 한글은 완성형만 허용한다. 자음·모음 단독(ㄱ, ㅏ)은 닉네임으로 쓰지 않는다. */
        private val ALLOWED = Regex("[가-힣a-zA-Z0-9_-]+")

        fun isValid(value: String): Boolean =
            hasValidLength(value) && hasAllowedCharactersOnly(value)

        private fun hasValidLength(value: String): Boolean = value.length in MIN_LENGTH..MAX_LENGTH

        private fun hasAllowedCharactersOnly(value: String): Boolean = ALLOWED.matches(value)
    }
}
