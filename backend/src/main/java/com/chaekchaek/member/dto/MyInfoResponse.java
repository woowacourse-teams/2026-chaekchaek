package com.chaekchaek.member.dto;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.common.auth.ActorType;
import com.chaekchaek.member.domain.Member;

public record MyInfoResponse(
        Long memberId,
        Long actorId,
        ActorType actorType,
        String nickname,
        String anonymousNickname,
        String profileImageUrl,
        boolean displayAnonymous,
        String accountStatus
) {
    public static MyInfoResponse from(Member member, Actor actor) {
        return new MyInfoResponse(
                member.getId(),
                actor.getId(),
                actor.getType(),
                member.getNickname(),
                member.getAnonymousNickname(),
                member.getProfileImageUrl(),
                member.isDisplayAnonymous(),
                member.getAccountStatus().name()
        );
    }
}
