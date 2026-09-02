package com.chaekchaek.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.chaekchaek.book.client.AladinClientException;
import com.chaekchaek.book.client.BookClientException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    @DisplayName("비즈니스 예외의 상태 코드와 에러 응답을 반환한다")
    void should_ReturnProblemDetail_When_BusinessExceptionOccurs() {
        // given
        BusinessException exception =
                new MemberNotFoundException();

        // when
        ProblemDetail response =
                handler.handleBusinessException(exception);

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
    @DisplayName("외부 도서 API 예외에 게이트웨이 오류 응답을 반환한다")
    void should_ReturnBadGateway_When_BookClientExceptionOccurs() {
        // given
        BookClientException exception = new AladinClientException(
                new IllegalStateException("external detail"));

        // when
        ProblemDetail response = handler.handleBookClientException(exception);

        // then
        assertAll(
                () -> assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value()),
                () -> assertThat(response.getDetail()).isEqualTo(ErrorCode.EXTERNAL_API_ERROR.getMessage()),
                () -> assertThat(response.getDetail()).doesNotContain("external detail"),
                () -> assertThat(response.getProperties())
                        .containsEntry("code", ErrorCode.EXTERNAL_API_ERROR.getCode())
        );
    }

    @Test
    @DisplayName("예상하지 못한 예외에 서버 오류 응답을 반환한다")
    void should_ReturnInternalServerError_When_UnexpectedExceptionOccurs() {
        // given
        Exception exception = new RuntimeException("internal detail");

        // when
        ProblemDetail response =
                handler.handleUnexpectedException(exception);

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
}
