package com.chaekchaek.auth.principal;

import com.chaekchaek.common.logging.RequestLogAttributes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.OptionalLong;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthenticatedMemberLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        recordAuthenticatedMember(request);
        filterChain.doFilter(request, response);
    }

    private void recordAuthenticatedMember(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return;
        }
        memberIdOf(jwt).ifPresent(memberId -> RequestLogAttributes.recordMember(request, memberId));
    }

    private OptionalLong memberIdOf(Jwt jwt) {
        try {
            return OptionalLong.of(Long.parseLong(jwt.getSubject()));
        } catch (NumberFormatException exception) {
            return OptionalLong.empty();
        }
    }
}
