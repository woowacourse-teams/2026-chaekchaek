package com.chaekchaek.auth.config;

public final class PublicEndpointPaths {

    public static final String[] GET_ENDPOINTS = {
            "/api/v1/home/**",
            "/api/v1/books",
            "/api/v1/books/**",
            "/api/v1/books/*/reviews",
            "/api/v1/reviews/*/replies"
    };

    private PublicEndpointPaths() {

    }
}
