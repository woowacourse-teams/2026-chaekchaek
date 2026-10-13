package com.chaekchaek.auth.principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.chaekchaek.common.logging.AccessLogFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AuthenticatedMemberLogFilterTest {

    private final AccessLogFilter accessLogFilter = new AccessLogFilter();
    private final AuthenticatedMemberLogFilter memberLogFilter = new AuthenticatedMemberLogFilter();
    private final Logger logger = (Logger) LoggerFactory.getLogger(AccessLogFilter.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("JWT로 인증된 요청이면 회원 ID를 접근 로그에 남긴다")
    void should_RecordMemberId_When_JwtAuthenticated() throws Exception {
        // given
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("7")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/library"
        );
        FilterChain securityChain = (servletRequest, servletResponse) ->
                memberLogFilter.doFilter(servletRequest, servletResponse, new MockFilterChain());

        // when
        accessLogFilter.doFilter(request, new MockHttpServletResponse(), securityChain);

        // then
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getKeyValuePairs())
                .extracting(pair -> pair.key, pair -> pair.value)
                .contains(
                        tuple("authType", "MEMBER"),
                        tuple("memberId", 7L)
                );
    }

    @Test
    @DisplayName("인증 정보가 없는 요청이면 익명으로 남긴다")
    void should_RecordAnonymous_When_NotAuthenticated() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/books"
        );
        FilterChain securityChain = (servletRequest, servletResponse) ->
                memberLogFilter.doFilter(servletRequest, servletResponse, new MockFilterChain());

        // when
        accessLogFilter.doFilter(request, new MockHttpServletResponse(), securityChain);

        // then
        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.getFirst();
        assertAll(
                () -> assertThat(event.getKeyValuePairs())
                        .extracting(pair -> pair.key, pair -> pair.value)
                        .contains(tuple("authType", "ANONYMOUS")),
                () -> assertThat(event.getKeyValuePairs())
                        .extracting(pair -> pair.key)
                        .doesNotContain("memberId")
        );
    }
}
