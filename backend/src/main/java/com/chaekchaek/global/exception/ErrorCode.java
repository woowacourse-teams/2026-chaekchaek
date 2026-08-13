package com.chaekchaek.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    MEMBER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "[ERROR] 회원이 존재해야 합니다"
    ),

    REFRESH_TOKEN_REQUIRED(
            HttpStatus.UNAUTHORIZED,
            "[ERROR] Refresh Token이 존재해야 합니다"
    ),

    INVALID_REFRESH_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "[ERROR] Refresh Token은 유효해야 합니다. 다시 로그인해 주세요"
    ),

    UNUSABLE_REFRESH_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "[ERROR] Refresh Token은 만료되거나 폐기되지 않은 상태여야 합니다. 다시 로그인해 주세요"
    ),

    UNAUTHORIZED(
            HttpStatus.UNAUTHORIZED,
            "[ERROR] 인증 정보가 유효해야 합니다. 다시 로그인해 주세요"
    );

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
