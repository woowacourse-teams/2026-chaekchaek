package com.chaekchaek.member.dto;

import com.chaekchaek.member.domain.Member;

public record MemberResponse(
        Long memberId,
        String nickname,
        String profileImageUrl,
        boolean displayAnonymous,
        String accountStatus
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getNickname(),
                member.getProfileImageUrl(),
                member.isDisplayAnonymous(),
                member.getAccountStatus().name()
        );
    }
}
