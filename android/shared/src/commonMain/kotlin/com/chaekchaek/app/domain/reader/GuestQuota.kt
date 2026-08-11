package com.chaekchaek.app.domain.reader

/**
 * 비로그인 상태에서 감상 전문을 몇 번 볼 수 있는지. Figma 홈 배너의 `지금 2 / 3`.
 *
 * 서버가 비로그인 사용자를 식별할 방법이 없어 기기에 저장한다. 재설치하면 리셋되는 것은
 * 감수한다(가벼운 가입 유도가 목적이라 엄격한 차단이 아니다).
 */
class GuestQuota(
    val viewed: Int,
    val limit: Int = DEFAULT_LIMIT,
) {
    init {
        require(viewed >= 0) { "열람 횟수는 0 이상이어야 합니다: $viewed" }
        require(limit > 0) { "열람 한도는 1 이상이어야 합니다: $limit" }
    }

    fun remaining(): Int = (limit - viewed).coerceAtLeast(0)

    fun isExhausted(): Boolean = viewed >= limit

    fun consumed(): GuestQuota = GuestQuota((viewed + 1).coerceAtMost(limit), limit)

    companion object {
        const val DEFAULT_LIMIT = 3
    }
}

/**
 * 지금 앱을 보고 있는 사람. 게스트는 쿼터가 있고 프로필이 없다. 회원은 그 반대다.
 */
sealed interface Viewer {
    data class Guest(val quota: GuestQuota) : Viewer

    data class Member(val profile: ReaderProfile) : Viewer
}
