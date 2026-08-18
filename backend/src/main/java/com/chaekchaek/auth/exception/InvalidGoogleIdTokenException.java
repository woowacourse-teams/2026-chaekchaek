package com.chaekchaek.auth.exception;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;

public class InvalidGoogleIdTokenException extends BusinessException {

    public InvalidGoogleIdTokenException() {
        super(ErrorCode.INVALID_GOOGLE_ID_TOKEN);
    }

    public InvalidGoogleIdTokenException(Throwable cause) {
        super(ErrorCode.INVALID_GOOGLE_ID_TOKEN);

        initCause(cause);
    }
}
