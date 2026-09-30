package com.chaekchaek.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);
    private static final String HEALTH_CHECK_PATH = "/health";
    private static final String ACTUATOR_PATH_PREFIX = "/actuator";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return HEALTH_CHECK_PATH.equals(path) || path.startsWith(ACTUATOR_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            logAccess(request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, startedAt);
            throw exception;
        }
        logAccess(request, response.getStatus(), startedAt);
    }

    private void logAccess(HttpServletRequest request, int status, long startedAt) {
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        log.info("HTTP request completed: method={}, path={}, status={}, durationMs={}",
                request.getMethod(), request.getRequestURI(), status, durationMs);
    }
}
