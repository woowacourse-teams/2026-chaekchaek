package com.chaekchaek.global.exception;

public record ErrorResponse(
        String code,
        String message
) {
}
