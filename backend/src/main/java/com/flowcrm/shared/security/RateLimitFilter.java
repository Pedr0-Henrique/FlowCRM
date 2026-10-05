package com.flowcrm.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final int loginAttempts;
    private final int registerAttempts;
    private final int refreshAttempts;
    private final long windowSeconds;
    private final RateLimitCounterStore counterStore;

    public RateLimitFilter(
            @Value("${app.security.rate-limit.login-attempts:10}") int loginAttempts,
            @Value("${app.security.rate-limit.register-attempts:5}") int registerAttempts,
            @Value("${app.security.rate-limit.refresh-attempts:20}") int refreshAttempts,
            @Value("${app.security.rate-limit.window-seconds:60}") long windowSeconds,
            RateLimitCounterStore counterStore) {
        if (loginAttempts < 1 || registerAttempts < 1 || refreshAttempts < 1 || windowSeconds < 1) {
            throw new IllegalArgumentException("Rate-limit values must be positive");
        }

        this.loginAttempts = loginAttempts;
        this.registerAttempts = registerAttempts;
        this.refreshAttempts = refreshAttempts;
        this.windowSeconds = windowSeconds;
        this.counterStore = counterStore;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        int limit = limitFor(request);
        if (limit == 0) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitCounterStore.RateLimitDecision decision = counterStore.consume(
                request.getRemoteAddr(),
                request.getRequestURI(),
                limit,
                windowSeconds
        );
        if (decision.attempts() > limit) {
            log.warn(
                    "event=rate_limit_exceeded path={} remoteAddr={} retryAfterSeconds={}",
                    request.getRequestURI(),
                    request.getRemoteAddr(),
                    decision.retryAfterSeconds()
            );
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"message\":\"Muitas tentativas. Tente novamente mais tarde.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private int limitFor(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return 0;
        }
        return switch (request.getRequestURI()) {
            case "/api/v1/auth/login" -> loginAttempts;
            case "/api/v1/auth/register" -> registerAttempts;
            case "/api/v1/auth/refresh" -> refreshAttempts;
            default -> 0;
        };
    }
}
