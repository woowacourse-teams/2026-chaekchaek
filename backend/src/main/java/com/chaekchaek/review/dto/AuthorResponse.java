package com.chaekchaek.review.dto;

import com.chaekchaek.common.auth.ActorType;

public record AuthorResponse(String displayName, String profileImageUrl, boolean anonymous, boolean mine,
                             ActorType actorType) {
}
