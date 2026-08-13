package com.chaekchaek.common.exception;

import com.chaekchaek.book.client.AladinClientException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class,
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            ConstraintViolationException.class
    })
    public ProblemDetail handleInvalidRequest(Exception exception) {
        return createProblemDetail(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AladinClientException.class)
    public ProblemDetail handleAladinClientException(AladinClientException exception) {
        log.warn("Aladin API call failed", exception);
        return createProblemDetail(ErrorCode.EXTERNAL_API_ERROR, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception exception) {
        log.error("Unexpected server error", exception);
        return createProblemDetail(ErrorCode.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ProblemDetail createProblemDetail(ErrorCode errorCode, HttpStatus status) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                status, errorCode.getMessage());
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setProperty("code", errorCode.getCode());
        return problemDetail;
    }
}
