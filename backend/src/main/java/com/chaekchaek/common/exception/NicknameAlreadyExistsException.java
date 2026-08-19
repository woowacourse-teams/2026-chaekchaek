package com.chaekchaek.common.exception;

public class NicknameAlreadyExistsException extends BusinessException {

    public NicknameAlreadyExistsException() {
        super(ErrorCode.NICKNAME_ALREADY_EXISTS);
    }
}
