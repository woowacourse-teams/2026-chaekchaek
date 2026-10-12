package com.chaekchaek.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.chaekchaek.auth.exception.AppleAuthServerException;
import com.chaekchaek.book.client.BookClientException;
import com.chaekchaek.book.client.Yes24ClientException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest(
            "GET",
            "/api/v1/members/me"
    );
    private final Logger logger = (Logger) LoggerFactory.getLogger(ApiExceptionHandler.class);
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
    @DisplayName("도서 외부 API 예외에 게이트웨이 오류 응답을 반환한다")
    void should_ReturnBadGateway_When_BookClientExceptionOccurs() {
        // given
        BookClientException exception = new Yes24ClientException(new RuntimeException("internal detail"));

        // when
        ProblemDetail response = handler.handleBookClientException(exception, request);

        // then
        assertAll(
                () -> assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value()),
                () -> assertThat(response.getDetail()).isEqualTo(ErrorCode.EXTERNAL_API_ERROR.getMessage()),
                () -> assertThat(response.getDetail()).doesNotContain("internal detail"),
                () -> assertThat(response.getProperties())
                        .containsEntry("code", ErrorCode.EXTERNAL_API_ERROR.getCode())
        );
    }

    @Test
    @DisplayName("비즈니스 예외의 상태 코드와 에러 응답을 반환한다")
    void should_ReturnProblemDetail_When_BusinessExceptionOccurs() {
        // given
        BusinessException exception =
                new MemberNotFoundException();

        // when
        ProblemDetail response =
                handler.handleBusinessException(exception, request);

        // then
        assertAll(
                () -> assertThat(response.getStatus()).isEqualTo(404),
                () -> assertThat(response.getDetail())
                        .isEqualTo(
                                ErrorCode.MEMBER_NOT_FOUND.getMessage()
                        ),
                () -> assertThat(response.getProperties())
                        .containsEntry(
                        "code",
                        ErrorCode.MEMBER_NOT_FOUND.getCode()
                )
        );
    }

    @Test
    @DisplayName("4xx 비즈니스 예외는 요청 정보와 에러 코드를 필드로 기록하고 스택트레이스는 남기지 않는다")
    void should_LogFieldsWithoutStackTrace_When_ClientBusinessExceptionOccurs() {
        // given
        BusinessException exception = new MemberNotFoundException();

        // when
        handler.handleBusinessException(exception, request);

        // then
        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.getFirst();
        assertAll(
                () -> assertThat(event.getLevel()).isEqualTo(Level.INFO),
                () -> assertThat(event.getFormattedMessage()).isEqualTo("Business exception"),
                () -> assertThat(event.getKeyValuePairs())
                        .extracting(pair -> pair.key, pair -> pair.value)
                        .containsExactly(
                                tuple("method", "GET"),
                                tuple("path", "/api/v1/members/me"),
                                tuple("code", ErrorCode.MEMBER_NOT_FOUND.getCode())
                        ),
                () -> assertThat(event.getThrowableProxy()).isNull()
        );
    }

    @Test
    @DisplayName("5xx 비즈니스 예외는 원인 예외와 함께 경고로 기록한다")
    void should_LogWarnWithCause_When_ServerBusinessExceptionOccurs() {
        // given
        BusinessException exception = new AppleAuthServerException(new RuntimeException("apple timeout"));

        // when
        ProblemDetail response = handler.handleBusinessException(exception, request);

        // then
        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.getFirst();
        assertAll(
                () -> assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value()),
                () -> assertThat(event.getLevel()).isEqualTo(Level.WARN),
                () -> assertThat(event.getKeyValuePairs())
                        .extracting(pair -> pair.key, pair -> pair.value)
                        .contains(tuple("code", ErrorCode.APPLE_AUTH_SERVER_ERROR.getCode())),
                () -> assertThat(event.getThrowableProxy().getCause().getMessage()).isEqualTo("apple timeout")
        );
    }

    @Test
    @DisplayName("예상하지 못한 예외에 서버 오류 응답을 반환한다")
    void should_ReturnInternalServerError_When_UnexpectedExceptionOccurs() {
        // given
        Exception exception = new RuntimeException("internal detail");

        // when
        ProblemDetail response =
                handler.handleUnexpectedException(exception, request);

        // then
        assertAll(
                () -> assertThat(response.getStatus())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                () -> assertThat(response.getDetail())
                        .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()),
                () -> assertThat(response.getDetail())
                        .doesNotContain("internal detail"),
                () -> assertThat(response.getProperties())
                        .containsEntry(
                        "code",
                        ErrorCode.INTERNAL_SERVER_ERROR.getCode()
                )
        );
    }

    @Test
    @DisplayName("예상하지 못한 예외는 요청 정보와 함께 에러로 기록한다")
    void should_LogErrorWithRequest_When_UnexpectedExceptionOccurs() {
        // given
        Exception exception = new RuntimeException("internal detail");

        // when
        handler.handleUnexpectedException(exception, request);

        // then
        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.getFirst();
        assertAll(
                () -> assertThat(event.getLevel()).isEqualTo(Level.ERROR),
                () -> assertThat(event.getFormattedMessage()).isEqualTo("Unexpected server error"),
                () -> assertThat(event.getKeyValuePairs())
                        .extracting(pair -> pair.key, pair -> pair.value)
                        .containsExactly(
                                tuple("method", "GET"),
                                tuple("path", "/api/v1/members/me")
                        ),
                () -> assertThat(event.getThrowableProxy().getMessage()).isEqualTo("internal detail")
        );
    }
}
