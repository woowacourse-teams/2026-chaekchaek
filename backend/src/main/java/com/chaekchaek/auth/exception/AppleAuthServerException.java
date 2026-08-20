package com.chaekchaek.auth.exception;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;

public class AppleAuthServerException extends BusinessException {

    public AppleAuthServerException(Throwable cause) {
        super(ErrorCode.APPLE_AUTH_SERVER_ERROR);
        initCause(cause);
    }
}
