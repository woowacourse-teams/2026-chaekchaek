package com.chaekchaek.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

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
    @DisplayName("요청이 끝나면 메서드, 경로, 상태 코드, 처리 시간을 필드로 기록한다")
    void should_LogRequestFields_When_RequestCompletes() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/books"
        );
        request.setQueryString("query=secret");
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
                                tuple("method", "GET"),
                                tuple("path", "/api/v1/books"),
                                tuple("status", 200)
                        ),
                () -> assertThat(event.getKeyValuePairs())
                        .filteredOn(pair -> pair.key.equals("durationMs"))
                        .extracting(pair -> pair.value)
                        .hasOnlyElementsOfType(Long.class)
                        .hasSize(1),
                () -> assertThat(event.getKeyValuePairs())
                        .extracting(pair -> pair.value.toString())
                        .noneMatch(value -> value.contains("secret"))
        );
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
                        )
        );
    }
}
