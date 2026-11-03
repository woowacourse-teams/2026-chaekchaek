package com.chaekchaek.auth.principal;

import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import java.util.OptionalLong;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityContextCurrentMemberIdProvider implements CurrentMemberIdProvider {

    private final MemberRepository memberRepository;

    @Override
    public long getCurrentMemberId() {
        return findCurrentMemberId()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    @Override
    public long getCurrentMemberIdForWrite() {
        long memberId = findAuthenticatedMemberId()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (member.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return memberId;
    }

    @Override
    public OptionalLong findCurrentMemberId() {
        OptionalLong memberId = findAuthenticatedMemberId();
        if (memberId.isEmpty()) {
            return OptionalLong.empty();
        }
        memberRepository.findById(memberId.getAsLong())
                .filter(member -> member.getAccountStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return memberId;
    }

    private OptionalLong findAuthenticatedMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return OptionalLong.empty();
        }
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(Long.parseLong(jwt.getSubject()));
        } catch (NumberFormatException exception) {
            return OptionalLong.empty();
        }
    }
}
