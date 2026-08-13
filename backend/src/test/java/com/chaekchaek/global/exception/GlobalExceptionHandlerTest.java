package com.chaekchaek.global.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("비즈니스 예외의 상태 코드와 에러 응답을 반환한다")
    void should_ReturnErrorResponse_When_BusinessExceptionOccurs() {
        // given
        BusinessException exception = new BusinessException(
                ErrorCode.MEMBER_NOT_FOUND
        );

        // when
        ResponseEntity<ErrorResponse> response =
                handler.handleBusinessException(exception);

        // then
        assertAll(
                () -> assertThat(response.getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND),
                () -> assertThat(response.getBody().code())
                        .isEqualTo("MEMBER_NOT_FOUND"),
                () -> assertThat(response.getBody().message())
                        .isEqualTo(ErrorCode.MEMBER_NOT_FOUND.getMessage())
        );
    }

    @Test
    @DisplayName("예상하지 못한 예외에 서버 오류 응답을 반환한다")
    void should_ReturnInternalServerError_When_UnexpectedExceptionOccurs() {
        // given
        Exception exception = new RuntimeException("internal detail");

        // when
        ResponseEntity<ErrorResponse> response =
                handler.handleException(exception);

        // then
        assertAll(
                () -> assertThat(response.getStatusCode())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR),
                () -> assertThat(response.getBody().code())
                        .isEqualTo("INTERNAL_SERVER_ERROR"),
                () -> assertThat(response.getBody().message())
                        .doesNotContain("internal detail")
        );
    }
}
