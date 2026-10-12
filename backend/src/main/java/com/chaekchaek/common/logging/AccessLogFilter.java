package com.chaekchaek.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessLogFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);
    private static final String HEALTH_CHECK_PATH = "/health";
    private static final String ACTUATOR_PATH_PREFIX = "/actuator";
    private static final String UNMATCHED_ROUTE = "UNMATCHED";
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return HEALTH_CHECK_PATH.equals(path) || path.startsWith(ACTUATOR_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        String requestId = resolveRequestId(request);
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
            logAccess(request, response.getStatus(), startedAt);
        } catch (IOException | ServletException | RuntimeException exception) {
            logAccess(request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, startedAt);
            throw exception;
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId != null && SAFE_REQUEST_ID.matcher(requestId).matches()) {
            return requestId;
        }
        return UUID.randomUUID().toString();
    }

    private void logAccess(HttpServletRequest request, int status, long startedAt) {
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        LoggingEventBuilder event = log.atInfo()
                .addKeyValue("type", "access")
                .addKeyValue("method", request.getMethod())
                .addKeyValue("route", route(request))
                .addKeyValue("path", request.getRequestURI())
                .addKeyValue("status", status)
                .addKeyValue("durationMs", durationMs)
                .addKeyValue("authType", RequestLogAttributes.authType(request));
        Object actorId = request.getAttribute(RequestLogAttributes.ACTOR_ID);
        if (actorId != null) {
            event = event.addKeyValue("actorId", actorId);
        }
        Object memberId = request.getAttribute(RequestLogAttributes.MEMBER_ID);
        if (memberId != null) {
            event = event.addKeyValue("memberId", memberId);
        }
        event.log("HTTP request completed");
    }

    private String route(HttpServletRequest request) {
        Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (route == null) {
            return UNMATCHED_ROUTE;
        }
        return route.toString();
    }
}
