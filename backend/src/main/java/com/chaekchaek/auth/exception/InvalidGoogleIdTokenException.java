package com.chaekchaek.auth.exception;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class InvalidGoogleIdTokenException extends BusinessException {

    public InvalidGoogleIdTokenException() {
        super(
                ErrorCode.INVALID_GOOGLE_ID_TOKEN,
                HttpStatus.UNAUTHORIZED
        );
    }

    public InvalidGoogleIdTokenException(Throwable cause) {
        super(
                ErrorCode.INVALID_GOOGLE_ID_TOKEN,
                HttpStatus.UNAUTHORIZED
        );

        initCause(cause);
    }
}
