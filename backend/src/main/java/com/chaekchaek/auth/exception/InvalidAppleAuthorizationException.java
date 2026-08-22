package com.chaekchaek.auth.exception;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;

public class InvalidAppleAuthorizationException extends BusinessException {

    public InvalidAppleAuthorizationException() {
        super(ErrorCode.INVALID_APPLE_AUTHORIZATION);
    }

    public InvalidAppleAuthorizationException(Throwable cause) {
        super(ErrorCode.INVALID_APPLE_AUTHORIZATION);
        initCause(cause);
    }
}
