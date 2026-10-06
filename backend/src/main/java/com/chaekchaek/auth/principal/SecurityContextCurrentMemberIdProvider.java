package com.chaekchaek.auth.principal;

import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.member.domain.AccountStatus;
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
    public OptionalLong findCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return OptionalLong.empty();
        }
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return OptionalLong.empty();
        }
        try {
            long memberId = Long.parseLong(jwt.getSubject());
            memberRepository.findById(memberId)
                    .filter(member -> member.getAccountStatus() == AccountStatus.ACTIVE)
                    .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
            return OptionalLong.of(memberId);
        } catch (NumberFormatException exception) {
            return OptionalLong.empty();
        }
    }
}
