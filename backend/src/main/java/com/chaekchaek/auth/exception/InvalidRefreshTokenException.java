package com.chaekchaek.auth.exception;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;

public class InvalidRefreshTokenException extends BusinessException {

    public InvalidRefreshTokenException(ErrorCode errorCode) {
        super(errorCode);
    }
}
