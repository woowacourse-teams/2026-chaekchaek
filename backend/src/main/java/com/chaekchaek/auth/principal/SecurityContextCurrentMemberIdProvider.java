package com.chaekchaek.auth.principal;

import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import java.util.OptionalLong;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextCurrentMemberIdProvider implements CurrentMemberIdProvider {

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
            return OptionalLong.of(Long.parseLong(jwt.getSubject()));
        } catch (NumberFormatException exception) {
            return OptionalLong.empty();
        }
    }
}
