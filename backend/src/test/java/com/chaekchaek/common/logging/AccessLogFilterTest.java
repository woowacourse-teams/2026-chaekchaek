package com.chaekchaek.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(OutputCaptureExtension.class)
class AccessLogFilterTest {

    private final AccessLogFilter filter = new AccessLogFilter();

    @Test
    @DisplayName("요청이 끝나면 메서드, 경로, 상태 코드, 처리 시간을 기록한다")
    void should_LogRequest_When_RequestCompletes(CapturedOutput output) throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/books");
        request.setQueryString("query=secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        // when
        filter.doFilter(request, response, new MockFilterChain());

        // then
        assertAll(
                () -> assertThat(output).contains("method=GET, path=/api/v1/books, status=200, durationMs="),
                () -> assertThat(output).doesNotContain("query=secret")
        );
    }

    @Test
    @DisplayName("헬스 체크 요청은 기록하지 않는다")
    void should_NotLog_When_HealthCheckRequested(CapturedOutput output) throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, new MockFilterChain());

        // then
        assertThat(output).doesNotContain("HTTP request completed");
    }

    @Test
    @DisplayName("요청 처리 중 예외가 전파되면 500으로 기록하고 예외를 다시 던진다")
    void should_LogServerError_When_ExceptionPropagates(CapturedOutput output) {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/reviews");
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
                () -> assertThat(output).contains("method=POST, path=/api/v1/reviews, status=500")
        );
    }
}
