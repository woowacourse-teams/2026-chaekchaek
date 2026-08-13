package com.chaekchaek.review.member;

/** Values owned by Member/Auth and needed for Review's author projection. */
public record ReviewMemberProfile(
        String displayName,
        String profileImageUrl,
        String anonymousHandle,
        boolean anonymousEnabled,
        boolean withdrawn
) {
}
