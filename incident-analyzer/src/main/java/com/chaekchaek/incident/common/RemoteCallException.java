package com.chaekchaek.incident.common;

public class RemoteCallException extends RuntimeException {
    public RemoteCallException(String code) {
        super(code);
    }
}
