package com.chaekchaek.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_REQUEST(
            "INVALID_REQUEST",
            "요청값이 올바르지 않습니다."),
    MEMBER_NOT_FOUND(
            "MEMBER_NOT_FOUND",
            "회원을 찾을 수 없습니다."
    ),
    NICKNAME_ALREADY_EXISTS(
            "NICKNAME_ALREADY_EXISTS",
            "이미 사용 중인 닉네임입니다."
    ),
    NICKNAME_REQUIRED(
            "NICKNAME_REQUIRED",
            "닉네임을 설정해야 익명 상태를 해제할 수 있습니다."
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
    INVALID_GUEST_TOKEN(
            "INVALID_GUEST_TOKEN",
            "게스트 토큰이 유효하지 않습니다."
    ),
    UNUSABLE_GUEST_TOKEN(
            "UNUSABLE_GUEST_TOKEN",
            "게스트 토큰이 만료되었거나 폐기되었습니다."
    ),
    INVALID_GOOGLE_ID_TOKEN(
            "INVALID_GOOGLE_ID_TOKEN",
            "Google 인증 정보가 유효하지 않습니다."
    ),
    INVALID_APPLE_AUTHORIZATION(
            "INVALID_APPLE_AUTHORIZATION",
            "Apple 인증 정보가 유효하지 않습니다."
    ),
    APPLE_AUTH_SERVER_ERROR(
            "APPLE_AUTH_SERVER_ERROR",
            "Apple 인증 서버와 통신하지 못했습니다."
    ),
    UNAUTHORIZED(
            "UNAUTHORIZED",
            "인증이 필요합니다."),
    FORBIDDEN(
            "FORBIDDEN",
            "요청한 작업을 수행할 권한이 없습니다."),
    BOOK_NOT_FOUND(
            "BOOK_NOT_FOUND",
            "책을 찾을 수 없습니다."),
    REVIEW_NOT_FOUND(
            "REVIEW_NOT_FOUND",
            "감상을 찾을 수 없습니다."),
    REPLY_NOT_FOUND(
            "REPLY_NOT_FOUND",
            "답글을 찾을 수 없습니다."),
    LIBRARY_ITEM_NOT_FOUND(
            "LIBRARY_ITEM_NOT_FOUND",
            "서재 항목을 찾을 수 없습니다."),
    LIBRARY_ITEM_ALREADY_EXISTS(
            "LIBRARY_ITEM_ALREADY_EXISTS",
            "이미 서재에 등록된 책입니다."),
    REACTION_ALREADY_EXISTS(
            "REACTION_ALREADY_EXISTS",
            "이미 좋아요를 남겼습니다."),
    TOTAL_PAGES_CONFLICT(
            "TOTAL_PAGES_CONFLICT",
            "전체 페이지 수가 기존 정보와 다릅니다."),
    DELETED_RESOURCE(
            "DELETED_RESOURCE",
            "삭제된 리소스입니다."),
    INVALID_READING_STATE(
            "INVALID_READING_STATE",
            "현재 읽기 상태에서는 요청을 처리할 수 없습니다."),
    EXTERNAL_API_ERROR(
            "EXTERNAL_API_ERROR",
            "외부 서비스 호출에 실패했습니다."),
    INTERNAL_SERVER_ERROR(
            "INTERNAL_SERVER_ERROR",
            "서버 내부 오류가 발생했습니다.");

    private final String code;
    private final String message;
}
