package com.chaekchaek.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OAuthFrontendRedirectResolver {

    private static final String CLIENT_SESSION_ATTRIBUTE =
            OAuthFrontendRedirectResolver.class.getName() + ".CLIENT";
    private static final String LOGIN_FAILURE_CODE = "OAUTH_LOGIN_FAILED";

    private final String defaultBaseUrl;
    private final String localBaseUrl;
    private final String devBaseUrl;
    private final String successPath;
    private final String failurePath;

    public OAuthFrontendRedirectResolver(
            @Value("${app.frontend.base-url}") String defaultBaseUrl,
            @Value("${app.frontend.local-base-url}") String localBaseUrl,
            @Value("${app.frontend.dev-base-url}") String devBaseUrl,
            @Value("${app.frontend.oauth-success-path}") String successPath,
            @Value("${app.frontend.oauth-failure-path}") String failurePath
    ) {
        this.defaultBaseUrl = defaultBaseUrl;
        this.localBaseUrl = localBaseUrl;
        this.devBaseUrl = devBaseUrl;
        this.successPath = successPath;
        this.failurePath = failurePath;
    }

    public void rememberClient(HttpServletRequest request, OAuthFrontendClient client) {
        request.getSession(true).setAttribute(CLIENT_SESSION_ATTRIBUTE, client);
    }

    public String resolveSuccessUrl(HttpServletRequest request) {
        return resolveBaseUrlAndClear(request) + successPath;
    }

    public String resolveFailureUrl(HttpServletRequest request) {
        return resolveBaseUrlAndClear(request)
                + failurePath
                + "?error="
                + LOGIN_FAILURE_CODE;
    }

    private String resolveBaseUrlAndClear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return defaultBaseUrl;
        }

        Object clientAttribute = session.getAttribute(CLIENT_SESSION_ATTRIBUTE);
        session.removeAttribute(CLIENT_SESSION_ATTRIBUTE);

        if (!(clientAttribute instanceof OAuthFrontendClient client)) {
            return defaultBaseUrl;
        }

        return switch (client) {
            case LOCAL -> localBaseUrl;
            case DEV -> devBaseUrl;
        };
    }
}
