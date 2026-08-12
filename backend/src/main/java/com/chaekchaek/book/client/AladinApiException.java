package com.chaekchaek.book.client;

import lombok.Getter;

@Getter
public class AladinApiException extends RuntimeException {

    private final int errorCode;
    private final String errorMessage;

    public AladinApiException(int errorCode, String errorMessage) {
        super("Aladin API error: code=%d, message=%s".formatted(errorCode, errorMessage));
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
