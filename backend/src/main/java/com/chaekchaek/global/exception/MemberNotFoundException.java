package com.chaekchaek.global.exception;

public class MemberNotFoundException extends BusinessException {

    public MemberNotFoundException() {
        super(ErrorCode.MEMBER_NOT_FOUND);
    }
}
