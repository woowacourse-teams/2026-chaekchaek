package com.chaekchaek.review.member;

import com.chaekchaek.common.auth.ActorType;
import com.chaekchaek.member.domain.AccountStatus;

/** Values owned by Member/Auth and needed for Review's author projection. */
public record ReviewMemberProfile(
        Long memberId,
        String displayName,
        String profileImageUrl,
        String anonymousNickname,
        boolean anonymousEnabled,
        AccountStatus accountStatus,
        ActorType actorType
) {
    public ReviewMemberProfile(String displayName, String profileImageUrl, String anonymousNickname,
                               boolean anonymousEnabled, boolean withdrawn, ActorType actorType) {
        this(null, displayName, profileImageUrl, anonymousNickname, anonymousEnabled,
                actorType == ActorType.GUEST ? null
                        : withdrawn ? AccountStatus.WITHDRAWN : AccountStatus.ACTIVE,
                actorType);
    }
}
