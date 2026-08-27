package com.chaekchaek.member.controller;

import static com.chaekchaek.auth.token.cookie.AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NicknameUpdateAccessLogFilter extends OncePerRequestFilter {

    private static final String NICKNAME_UPDATE_PATH = "/api/v1/members/me/nickname";
    private static final Logger log = LoggerFactory.getLogger(NicknameUpdateAccessLogFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.PATCH.matches(request.getMethod())
                || !NICKNAME_UPDATE_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authSource = authenticationSource(request);
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        log.info("Nickname update request entered: method={}, path={}, authSource={}, userAgent={}",
                request.getMethod(), request.getRequestURI(), authSource, userAgent);

        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info("Nickname update request exited: status={}, authSource={}, userAgent={}",
                    response.getStatus(), authSource, userAgent);
        }
    }

    private static String authenticationSource(HttpServletRequest request) {
        if (request.getHeader(HttpHeaders.AUTHORIZATION) != null) {
            return "AUTHORIZATION_HEADER";
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return "NONE";
        }

        for (Cookie cookie : cookies) {
            if (ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return "ACCESS_TOKEN_COOKIE";
            }
        }

        return "NONE";
    }
}
