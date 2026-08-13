package com.chaekchaek.auth.exception;

import com.chaekchaek.global.exception.BusinessException;
import com.chaekchaek.global.exception.ErrorCode;

public class InvalidRefreshTokenException extends BusinessException {

    public InvalidRefreshTokenException(ErrorCode errorCode) {
        super(errorCode);
    }
}
