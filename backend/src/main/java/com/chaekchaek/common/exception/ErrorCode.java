package com.chaekchaek.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_REQUEST(
            "INVALID_REQUEST",
            "요청값이 올바르지 않습니다."
    ),

    MEMBER_NOT_FOUND(
            "MEMBER_NOT_FOUND",
            "회원을 찾을 수 없습니다."
    ),

    REFRESH_TOKEN_REQUIRED(
            "REFRESH_TOKEN_REQUIRED",
            "Refresh Token이 필요합니다."
    ),

    INVALID_REFRESH_TOKEN(
            "INVALID_REFRESH_TOKEN",
            "Refresh Token이 유효하지 않습니다."
    ),

    UNUSABLE_REFRESH_TOKEN(
            "UNUSABLE_REFRESH_TOKEN",
            "Refresh Token이 만료되었거나 폐기되었습니다."
    ),

    UNAUTHORIZED(
            "UNAUTHORIZED",
            "인증 정보가 유효하지 않습니다."
    ),

    EXTERNAL_API_ERROR(
            "EXTERNAL_API_ERROR",
            "외부 서비스 호출에 실패했습니다."
    ),

    INTERNAL_SERVER_ERROR(
            "INTERNAL_SERVER_ERROR",
            "서버 내부 오류가 발생했습니다."
    );

    private final String code;
    private final String message;
}
