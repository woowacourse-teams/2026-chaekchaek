package com.chaekchaek.book.client;

public final class Yes24ClientException extends BookClientException {

    public Yes24ClientException(Throwable cause) {
        super("Failed to call YES24 API", cause);
    }

    public Yes24ClientException(String errorCode) {
        super("YES24 API error: code=%s".formatted(errorCode));
    }
}
