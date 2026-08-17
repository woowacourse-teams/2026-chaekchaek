package com.chaekchaek.common.exception;

import org.springframework.http.HttpStatus;

public class MemberNotFoundException extends BusinessException {

    public MemberNotFoundException() {
        super(ErrorCode.MEMBER_NOT_FOUND, HttpStatus.NOT_FOUND);
    }
}
