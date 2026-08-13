package com.chaekchaek.review.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode {
    REVIEW_NOT_FOUND("REVIEW_NOT_FOUND", "감상을 찾을 수 없습니다."),
    REPLY_NOT_FOUND("REPLY_NOT_FOUND", "답글을 찾을 수 없습니다."),
    FORBIDDEN("FORBIDDEN", "요청을 수행할 권한이 없습니다."),
    DELETED_RESOURCE("DELETED_RESOURCE", "삭제된 리소스입니다."),
    REACTION_ALREADY_EXISTS("REACTION_ALREADY_EXISTS", "이미 좋아요를 남겼습니다."),
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다."),
    INVALID_REQUEST("INVALID_REQUEST", "요청값이 올바르지 않습니다.");

    private final String code;
    private final String message;
}
