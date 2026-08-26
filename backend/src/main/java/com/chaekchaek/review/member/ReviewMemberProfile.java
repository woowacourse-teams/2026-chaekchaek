package com.chaekchaek.review.member;

import com.chaekchaek.common.auth.ActorType;

/** Values owned by Member/Auth and needed for Review's author projection. */
public record ReviewMemberProfile(
        String displayName,
        String profileImageUrl,
        String anonymousNickname,
        boolean anonymousEnabled,
        boolean withdrawn,
        ActorType actorType
) {
}
