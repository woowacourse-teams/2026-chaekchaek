package com.chaekchaek.review.dto;

import com.chaekchaek.common.auth.ActorType;

public record AuthorResponse(Long memberId, String displayName, String profileImageUrl, boolean anonymous,
                             boolean mine, ActorType actorType, AuthorProfileStatus profileStatus) {

    public AuthorResponse(String displayName, String profileImageUrl, boolean anonymous, boolean mine,
                          ActorType actorType) {
        this(null, displayName, profileImageUrl, anonymous, mine, actorType, AuthorProfileStatus.UNAVAILABLE);
    }
}
