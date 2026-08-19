package com.chaekchaek.common.exception;

public class NicknameRequiredException extends BusinessException {

    public NicknameRequiredException() {
        super(ErrorCode.NICKNAME_REQUIRED);
    }
}
