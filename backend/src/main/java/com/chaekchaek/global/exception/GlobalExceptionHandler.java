package com.chaekchaek.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException exception
    ) {
        ErrorCode errorCode = exception.getErrorCode();

        ErrorResponse response = new ErrorResponse(
                errorCode.name(),
                errorCode.getMessage()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception exception
    ) {
        log.error("예상치 못한 예외가 발생했습니다", exception);

        ErrorResponse response = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "[ERROR] 서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요"
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
