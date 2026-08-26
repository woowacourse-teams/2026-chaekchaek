package com.chaekchaek.common.auth;

public record CurrentActor(long actorId, ActorType type, Long memberId) {

    public static CurrentActor member(long actorId, long memberId) {
        return new CurrentActor(actorId, ActorType.MEMBER, memberId);
    }

    public static CurrentActor guest(long actorId) {
        return new CurrentActor(actorId, ActorType.GUEST, null);
    }

    public boolean isMember() {
        return type == ActorType.MEMBER;
    }

    public boolean isGuest() {
        return type == ActorType.GUEST;
    }
}
