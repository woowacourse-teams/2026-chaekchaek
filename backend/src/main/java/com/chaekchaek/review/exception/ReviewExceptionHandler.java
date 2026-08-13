package com.chaekchaek.review.exception;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ReviewExceptionHandler {

    @ExceptionHandler(ReviewException.class)
    public ProblemDetail handleReviewException(ReviewException exception) {
        HttpStatus status = switch (exception.getErrorCode()) {
            case REVIEW_NOT_FOUND, REPLY_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case DELETED_RESOURCE, REACTION_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
        };
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setProperty("code", exception.getErrorCode().getCode());
        return problemDetail;
    }
}
