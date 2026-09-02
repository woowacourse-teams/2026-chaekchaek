package com.chaekchaek.library.dto;

import com.chaekchaek.member.domain.Member;

public record PublicMemberResponse(
        Long memberId,
        String displayName,
        String profileImageUrl
) {
    public static PublicMemberResponse from(Member member) {
        return new PublicMemberResponse(member.getId(), member.getDisplayName(), member.getProfileImageUrl());
    }
}
