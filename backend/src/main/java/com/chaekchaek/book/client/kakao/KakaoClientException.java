package com.chaekchaek.book.client.kakao;

import com.chaekchaek.book.client.BookClientException;

public final class KakaoClientException extends BookClientException {

    public KakaoClientException(Throwable cause) {
        super("Failed to call Kakao API", cause);
    }
}
