package com.chaekchaek.app.domain.reader

/**
 * 내 프로필. Figma 문구 `해제하면 닉네임을 설정해야 합니다` 가 그대로 불변식이 된다.
 *
 * sealed interface 로 익명/실명을 나누지 않은 이유는 닉네임이 계정 속성이기 때문이다. 익명으로
 * 돌렸다가 다시 실명으로 바꿀 때 재입력을 요구하지 않으려면 닉네임이 보존되어야 한다.
 */
class ReaderProfile(
    val id: ReaderId,
    val nickname: Nickname?,
    val publishesAnonymously: Boolean,
) {
    init {
        require(publishesAnonymously || nickname != null) {
            "익명 공개를 해제하려면 닉네임을 먼저 설정해야 합니다."
        }
    }

    /** 닉네임 설정 다이얼로그를 띄우지 않고 바로 실명으로 바꿀 수 있는가. */
    fun canRevealName(): Boolean = nickname != null

    fun revealName(): ReaderProfile = ReaderProfile(id, nickname, publishesAnonymously = false)

    fun hideName(): ReaderProfile = ReaderProfile(id, nickname, publishesAnonymously = true)

    fun changeNickname(newNickname: Nickname): ReaderProfile =
        ReaderProfile(id, newNickname, publishesAnonymously)
}
