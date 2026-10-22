package com.chaekchaek.common.exception;

import com.chaekchaek.book.client.BookClientException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
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
    public ProblemDetail handleInvalidRequest(Exception exception, HttpServletRequest request) {
        LoggingEventBuilder event = errorEvent(log.atInfo(), request, ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST)
                .addKeyValue("exception", exception.getClass().getSimpleName());
        List<String> invalidFields = invalidFields(exception);
        if (!invalidFields.isEmpty()) {
            event = event.addKeyValue("invalidFields", invalidFields);
        }
        event.log("잘못된 요청");
        return createProblemDetail(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
    }

    private List<String> invalidFields(Exception exception) {
        return switch (exception) {
            case MethodArgumentNotValidException invalid -> invalid.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getField)
                    .distinct()
                    .toList();
            case ConstraintViolationException invalid -> invalid.getConstraintViolations().stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .distinct()
                    .toList();
            case HandlerMethodValidationException invalid -> invalid.getParameterValidationResults().stream()
                    .map(result -> result.getMethodParameter().getParameterName())
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            case MissingServletRequestParameterException invalid -> List.of(invalid.getParameterName());
            case MethodArgumentTypeMismatchException invalid -> List.of(invalid.getName());
            default -> List.of();
        };
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException exception, HttpServletRequest request) {
        ErrorCode errorCode = exception.getErrorCode();
        HttpStatus status = statusOf(errorCode);
        if (status.is5xxServerError()) {
            errorEvent(log.atWarn(), request, errorCode, status)
                    .setCause(exception)
                    .log("비즈니스 예외");
        } else {
            errorEvent(log.atInfo(), request, errorCode, status)
                    .log("비즈니스 예외");
        }
        return createProblemDetail(errorCode, status);
    }

    private HttpStatus statusOf(ErrorCode errorCode) {
        return switch (errorCode) {
            case UNAUTHORIZED, REFRESH_TOKEN_REQUIRED, INVALID_REFRESH_TOKEN, UNUSABLE_REFRESH_TOKEN,
                 INVALID_GUEST_TOKEN, UNUSABLE_GUEST_TOKEN, INVALID_GOOGLE_ID_TOKEN -> HttpStatus.UNAUTHORIZED;
            case INVALID_APPLE_AUTHORIZATION -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case BOOK_NOT_FOUND, REVIEW_NOT_FOUND, REPLY_NOT_FOUND, LIBRARY_ITEM_NOT_FOUND, LIBRARY_NOT_FOUND,
                 MEMBER_NOT_FOUND,
                 RECOMMENDED_BOOK_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case LIBRARY_ITEM_ALREADY_EXISTS, LIBRARY_NAME_ALREADY_EXISTS,
                 REACTION_ALREADY_EXISTS, TOTAL_PAGES_CONFLICT, DELETED_RESOURCE,
                 NICKNAME_ALREADY_EXISTS, RECOMMENDED_BOOK_ALREADY_EXISTS, RECOMMENDED_BOOK_LIMIT_EXCEEDED,
                 GUEST_TOKEN_REFRESH_NOT_ALLOWED ->
                    HttpStatus.CONFLICT;
            case INVALID_READING_STATE, NICKNAME_REQUIRED -> HttpStatus.UNPROCESSABLE_CONTENT;
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case EXTERNAL_API_ERROR, APPLE_AUTH_SERVER_ERROR -> HttpStatus.BAD_GATEWAY;
            case INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    @ExceptionHandler(BookClientException.class)
    public ProblemDetail handleBookClientException(BookClientException exception, HttpServletRequest request) {
        errorEvent(log.atWarn(), request, ErrorCode.EXTERNAL_API_ERROR, HttpStatus.BAD_GATEWAY)
                .setCause(exception)
                .log("도서 API 호출 실패");
        return createProblemDetail(ErrorCode.EXTERNAL_API_ERROR, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception exception, HttpServletRequest request) {
        errorEvent(log.atError(), request, ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR)
                .addKeyValue("exception", exception.getClass().getSimpleName())
                .setCause(exception)
                .log("예상하지 못한 서버 오류");
        return createProblemDetail(
                ErrorCode.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private LoggingEventBuilder errorEvent(
            LoggingEventBuilder event,
            HttpServletRequest request,
            ErrorCode errorCode,
            HttpStatus status
    ) {
        return event
                .addKeyValue("type", "error")
                .addKeyValue("errorCode", errorCode.getCode())
                .addKeyValue("status", status.value())
                .addKeyValue("method", request.getMethod())
                .addKeyValue("path", request.getRequestURI());
    }

    private ProblemDetail createProblemDetail(ErrorCode errorCode, HttpStatus status) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                status,
                errorCode.getMessage()
        );
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setProperty("code", errorCode.getCode());
        return problemDetail;
    }
}
