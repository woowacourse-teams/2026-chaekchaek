package com.chaekchaek.common.exception;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(
            BusinessException exception
    ) {
        return createProblemDetail(
                exception.getErrorCode(),
                exception.getStatus()
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(
            Exception exception
    ) {
        log.error("Unexpected server error", exception);

        return createProblemDetail(
                ErrorCode.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private ProblemDetail createProblemDetail(
            ErrorCode errorCode,
            HttpStatus status
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                status, errorCode.getMessage()
        );

        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setProperty("code", errorCode.getCode());

        return problemDetail;
    }
}
