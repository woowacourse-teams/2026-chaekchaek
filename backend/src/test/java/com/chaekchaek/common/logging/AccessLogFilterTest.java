package com.chaekchaek.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.chaekchaek.common.auth.CurrentActor;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

class AccessLogFilterTest {

    private final AccessLogFilter filter = new AccessLogFilter();
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
    }

    @Test
    @DisplayName("요청이 끝나면 메서드, 라우트, 경로, 상태 코드, 처리 시간을 필드로 기록한다")
    void should_LogRequestFields_When_RequestCompletes() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/books/by-isbn/9791124038161"
        );
        request.setQueryString("query=secret");
        request.setAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                "/api/v1/books/by-isbn/{isbn13}"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        // when
        filter.doFilter(request, response, new MockFilterChain());

        // then
        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.getFirst();
        assertAll(
                () -> assertThat(event.getFormattedMessage()).isEqualTo("HTTP request completed"),
                () -> assertThat(event.getKeyValuePairs())
                        .extracting(pair -> pair.key, pair -> pair.value)
                        .contains(
                                tuple("type", "access"),
                                tuple("method", "GET"),
                                tuple("route", "/api/v1/books/by-isbn/{isbn13}"),
                                tuple("path", "/api/v1/books/by-isbn/9791124038161"),
                                tuple("status", 200),
                                tuple("authType", "ANONYMOUS")
                        ),
                () -> assertThat(event.getKeyValuePairs())
                        .filteredOn(pair -> pair.key.equals("durationMs"))
                        .extracting(pair -> pair.value)
                        .hasOnlyElementsOfType(Long.class)
                        .hasSize(1),
                () -> assertThat(event.getKeyValuePairs())
                        .extracting(pair -> pair.key)
                        .doesNotContain("actorId", "memberId"),
                () -> assertThat(event.getKeyValuePairs())
                        .extracting(pair -> pair.value.toString())
                        .noneMatch(value -> value.contains("secret"))
        );
    }

    @Test
    @DisplayName("매칭된 핸들러가 없으면 라우트를 UNMATCHED로 기록한다")
    void should_LogUnmatchedRoute_When_NoHandlerMatched() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/unknown"
        );

        // when
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        // then
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getKeyValuePairs())
                .extracting(pair -> pair.key, pair -> pair.value)
                .contains(tuple("route", "UNMATCHED"));
    }

    @Test
    @DisplayName("요청 처리 중 확인된 Actor 정보를 함께 기록한다")
    void should_LogActor_When_ActorRecorded() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/v1/reviews/1/reactions"
        );
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
                RequestLogAttributes.recordMember(request, 7L);
                RequestLogAttributes.recordActor(request, CurrentActor.admin(42L, 7L));
            }
        };

        // when
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // then
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getKeyValuePairs())
                .extracting(pair -> pair.key, pair -> pair.value)
                .contains(
                        tuple("authType", "ADMIN"),
                        tuple("actorId", 42L),
                        tuple("memberId", 7L)
                );
    }

    @Test
    @DisplayName("JWT 인증만 확인된 요청은 회원 ID와 MEMBER 인증 유형을 기록한다")
    void should_LogMember_When_OnlyMemberRecorded() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/library"
        );
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
                RequestLogAttributes.recordMember(request, 7L);
            }
        };

        // when
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // then
        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.getFirst();
        assertAll(
                () -> assertThat(event.getKeyValuePairs())
                        .extracting(pair -> pair.key, pair -> pair.value)
                        .contains(
                                tuple("authType", "MEMBER"),
                                tuple("memberId", 7L)
                        ),
                () -> assertThat(event.getKeyValuePairs())
                        .extracting(pair -> pair.key)
                        .doesNotContain("actorId")
        );
    }

    @Test
    @DisplayName("요청 ID를 생성해 응답 헤더와 요청 처리 중 MDC에 남기고, 요청이 끝나면 비운다")
    void should_PropagateRequestId_When_RequestProcessed() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/books"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInChain = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
                requestIdInChain.set(MDC.get(AccessLogFilter.REQUEST_ID_MDC_KEY));
            }
        };

        // when
        filter.doFilter(request, response, chain);

        // then
        String requestId = response.getHeader(AccessLogFilter.REQUEST_ID_HEADER);
        assertAll(
                () -> assertThat(requestId).isNotBlank(),
                () -> assertThat(requestIdInChain.get()).isEqualTo(requestId),
                () -> assertThat(MDC.get(AccessLogFilter.REQUEST_ID_MDC_KEY)).isNull()
        );
    }

    @Test
    @DisplayName("안전한 형식의 요청 ID 헤더가 오면 그대로 사용한다")
    void should_ReuseRequestId_When_SafeHeaderGiven() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/books"
        );
        request.addHeader(AccessLogFilter.REQUEST_ID_HEADER, "c59b3cf6-1a2b-4c3d-9e8f-000000000001");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, new MockFilterChain());

        // then
        assertThat(response.getHeader(AccessLogFilter.REQUEST_ID_HEADER))
                .isEqualTo("c59b3cf6-1a2b-4c3d-9e8f-000000000001");
    }

    @Test
    @DisplayName("안전하지 않은 형식의 요청 ID 헤더는 버리고 새로 생성한다")
    void should_GenerateRequestId_When_UnsafeHeaderGiven() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/books"
        );
        request.addHeader(AccessLogFilter.REQUEST_ID_HEADER, "forged\nlevel=ERROR");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, new MockFilterChain());

        // then
        assertThat(response.getHeader(AccessLogFilter.REQUEST_ID_HEADER))
                .isNotBlank()
                .doesNotContain("forged");
    }

    @Test
    @DisplayName("헬스 체크 요청은 기록하지 않는다")
    void should_NotLog_When_HealthCheckRequested() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/health"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, new MockFilterChain());

        // then
        assertThat(appender.list).isEmpty();
    }

    @Test
    @DisplayName("요청 처리 중 예외가 전파되면 500으로 기록하고 예외를 다시 던진다")
    void should_LogServerError_When_ExceptionPropagates() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/v1/reviews"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain failingChain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) {
                throw new IllegalStateException("boom");
            }
        };

        // when & then
        assertAll(
                () -> assertThatThrownBy(() -> filter.doFilter(request, response, failingChain))
                        .isInstanceOf(IllegalStateException.class),
                () -> assertThat(appender.list).hasSize(1),
                () -> assertThat(appender.list.getFirst().getKeyValuePairs())
                        .extracting(pair -> pair.key, pair -> pair.value)
                        .contains(
                                tuple("method", "POST"),
                                tuple("path", "/api/v1/reviews"),
                                tuple("status", 500)
                        ),
                () -> assertThat(MDC.get(AccessLogFilter.REQUEST_ID_MDC_KEY)).isNull()
        );
    }
}
