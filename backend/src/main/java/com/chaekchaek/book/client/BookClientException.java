package com.chaekchaek.book.client;

import java.net.SocketTimeoutException;
import org.springframework.web.client.RestClientResponseException;

public class BookClientException extends RuntimeException {

    private final String upstreamService;

    protected BookClientException(String upstreamService, String message) {
        super(message);
        this.upstreamService = upstreamService;
    }

    protected BookClientException(String upstreamService, String message, Throwable cause) {
        super(message, cause);
        this.upstreamService = upstreamService;
    }

    public String getFailureKind() {
        if (findCause(SocketTimeoutException.class) != null) {
            return "TIMEOUT";
        }
        if (getUpstreamStatus() != null) {
            return "HTTP_ERROR";
        }
        if (getCause() == null) {
            return "PROVIDER_ERROR";
        }
        return "UNKNOWN";
    }

    public String getUpstreamService() {
        return upstreamService;
    }

    public Integer getUpstreamStatus() {
        RestClientResponseException response = findCause(RestClientResponseException.class);
        if (response == null) {
            return null;
        }
        return response.getStatusCode().value();
    }

    private <T extends Throwable> T findCause(Class<T> type) {
        Throwable current = getCause();
        for (int depth = 0; current != null && depth < 16; depth++) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
