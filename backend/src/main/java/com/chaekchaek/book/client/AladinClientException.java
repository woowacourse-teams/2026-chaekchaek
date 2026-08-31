package com.chaekchaek.book.client;

public class AladinClientException extends BookClientException {

    public AladinClientException(Throwable cause) {
        super("Failed to call Aladin API", cause);
    }

    public AladinClientException(int aladinErrorCode, String aladinErrorMessage) {
        super("Aladin API error: code=%d, message=%s".formatted(
                aladinErrorCode, aladinErrorMessage));
    }
}
