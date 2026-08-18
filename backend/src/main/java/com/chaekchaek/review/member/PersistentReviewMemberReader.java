package com.chaekchaek.review.member;

import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PersistentReviewMemberReader implements ReviewMemberReader {

    private final MemberRepository memberRepository;

    @Override
    public Map<Long, ReviewMemberProfile> findByMemberIds(Collection<Long> memberIds) {
        return memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, this::toProfile));
    }

    private ReviewMemberProfile toProfile(Member member) {
        return new ReviewMemberProfile(
                member.getNickname(),
                member.getProfileImageUrl(),
                member.getAnonymousHandle(),
                member.isDisplayAnonymous(),
                member.getAccountStatus() == AccountStatus.WITHDRAWN
        );
    }
}
