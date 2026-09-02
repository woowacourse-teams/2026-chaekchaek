package com.chaekchaek.book.client;

public final class Yes24ClientException extends BookClientException {

    private final boolean fallbackAllowed;

    private Yes24ClientException(String message, Throwable cause, boolean fallbackAllowed) {
        super(message, cause);
        this.fallbackAllowed = fallbackAllowed;
    }

    static Yes24ClientException fallbackAllowed(Throwable cause) {
        return new Yes24ClientException("Failed to call YES24 API", cause, true);
    }

    static Yes24ClientException notFallbackAllowed(String message, Throwable cause) {
        return new Yes24ClientException(message, cause, false);
    }

    public boolean isFallbackAllowed() {
        return fallbackAllowed;
    }
}
