package com.chaekchaek.book.client;

public class BookClientException extends RuntimeException {

    protected BookClientException(String message) {
        super(message);
    }

    protected BookClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
