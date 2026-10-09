package com.chaekchaek.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.chaekchaek.auth.exception.AppleAuthServerException;
import com.chaekchaek.book.client.BookClientException;
import com.chaekchaek.book.client.Yes24ClientException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(OutputCaptureExtension.class)
class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest(
            "GET",
            "/api/v1/members/me"
    );

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
    @DisplayName("4xx 비즈니스 예외는 요청 정보와 에러 코드만 기록한다")
    void should_LogCodeWithoutStackTrace_When_ClientBusinessExceptionOccurs(CapturedOutput output) {
        // given
        BusinessException exception = new MemberNotFoundException();

        // when
        handler.handleBusinessException(exception, request);

        // then
        assertAll(
                () -> assertThat(output).contains(
                        "method=GET, path=/api/v1/members/me, code=" + ErrorCode.MEMBER_NOT_FOUND.getCode()),
                () -> assertThat(output).doesNotContain("MemberNotFoundException")
        );
    }

    @Test
    @DisplayName("5xx 비즈니스 예외는 원인 예외와 함께 경고로 기록한다")
    void should_LogWarnWithCause_When_ServerBusinessExceptionOccurs(CapturedOutput output) {
        // given
        BusinessException exception = new AppleAuthServerException(new RuntimeException("apple timeout"));

        // when
        ProblemDetail response = handler.handleBusinessException(exception, request);

        // then
        assertAll(
                () -> assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value()),
                () -> assertThat(output).contains("WARN"),
                () -> assertThat(output).contains("code=" + ErrorCode.APPLE_AUTH_SERVER_ERROR.getCode()),
                () -> assertThat(output).contains("apple timeout")
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
    void should_LogErrorWithRequest_When_UnexpectedExceptionOccurs(CapturedOutput output) {
        // given
        Exception exception = new RuntimeException("internal detail");

        // when
        handler.handleUnexpectedException(exception, request);

        // then
        assertAll(
                () -> assertThat(output).contains("ERROR"),
                () -> assertThat(output).contains("Unexpected server error: method=GET, path=/api/v1/members/me"),
                () -> assertThat(output).contains("internal detail")
        );
    }
}
