package com.chaekchaek.app.domain.profile

/** 서버가 공개한 식별자만 사용하며 익명 작성자의 실제 회원을 추론하지 않는다. */
data class AuthorProfile(
    val displayName: String,
    val memberId: Long? = null,
    val profileImageUrl: String? = null,
    val anonymous: Boolean = false,
    val actorType: String? = null,
    val profileStatus: String? = null,
) {
    val libraryAccess: LibraryAccess
        get() = when {
            actorType == "GUEST" -> LibraryAccess.Guest
            anonymous -> LibraryAccess.Anonymous
            actorType == "MEMBER" && profileStatus == "AVAILABLE" && memberId != null && memberId > 0 -> LibraryAccess.Available
            actorType == "MEMBER" && profileStatus == "UNAVAILABLE" -> LibraryAccess.Withdrawn
            else -> LibraryAccess.Unavailable
        }
    val visibleImageUrl: String? get() = profileImageUrl.takeIf { !anonymous && libraryAccess != LibraryAccess.Withdrawn }
}

enum class LibraryAccess(val message: String?) {
    Available(null),
    Anonymous("익명으로 작성한 사용자의 서재는 볼 수 없어요."),
    Guest("비회원으로 작성한 사용자는 서재가 없어요."),
    Withdrawn("탈퇴한 사용자의 서재는 볼 수 없어요."),
    Unavailable("이 사용자의 서재는 볼 수 없어요."),
}
