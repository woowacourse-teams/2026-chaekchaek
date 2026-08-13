package com.chaekchaek.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_REQUEST("INVALID_REQUEST", "요청값이 올바르지 않습니다."),
    EXTERNAL_API_ERROR("EXTERNAL_API_ERROR", "외부 서비스 호출에 실패했습니다."),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

    private final String code;
    private final String message;
}
