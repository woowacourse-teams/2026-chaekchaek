package com.chaekchaek.auth.token;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidRefreshTokenException extends RuntimeException{

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
