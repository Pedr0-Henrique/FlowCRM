package com.flowcrm.shared.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitFilterTest {

    @Test
    void limitsLoginAttemptsPerRemoteAddress() throws Exception {
        RateLimitCounterStore store = countingStore();
        RateLimitFilter filter = new RateLimitFilter(2, 1, 20, 60, store);
        AtomicInteger forwardedRequests = new AtomicInteger();
        FilterChain chain = (request, response) -> forwardedRequests.incrementAndGet();

        for (int attempt = 0; attempt < 2; attempt++) {
            MockHttpServletResponse response = invoke(filter, "/api/v1/auth/login", chain);
            assertEquals(200, response.getStatus());
        }

        MockHttpServletResponse limitedResponse = invoke(filter, "/api/v1/auth/login", chain);

        assertEquals(429, limitedResponse.getStatus());
        assertEquals("60", limitedResponse.getHeader("Retry-After"));
        assertEquals(2, forwardedRequests.get());
    }

    @Test
    void doesNotLimitUnrelatedRequests() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1, 1, 60, countingStore());
        AtomicInteger forwardedRequests = new AtomicInteger();

        MockHttpServletResponse response = invoke(
                filter,
                "/api/v1/clients",
                (request, result) -> forwardedRequests.incrementAndGet()
        );

        assertEquals(200, response.getStatus());
        assertEquals(1, forwardedRequests.get());
    }

    @Test
    void limitsRefreshRequestsSeparatelyFromLogins() throws Exception {
        RateLimitCounterStore store = countingStore();
        RateLimitFilter filter = new RateLimitFilter(10, 5, 1, 60, store);
        FilterChain chain = (request, response) -> {
        };

        assertEquals(200, invoke(filter, "/api/v1/auth/refresh", chain).getStatus());
        assertEquals(429, invoke(filter, "/api/v1/auth/refresh", chain).getStatus());
        assertEquals(200, invoke(filter, "/api/v1/auth/login", chain).getStatus());
    }

    private MockHttpServletResponse invoke(
            RateLimitFilter filter,
            String path,
            FilterChain chain
    ) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr("192.0.2.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    private RateLimitCounterStore countingStore() {
        java.util.concurrent.ConcurrentHashMap<String, AtomicInteger> attempts = new java.util.concurrent.ConcurrentHashMap<>();
        return (address, endpoint, limit, windowSeconds) -> {
            int attempt = attempts.computeIfAbsent(address + endpoint, ignored -> new AtomicInteger())
                    .incrementAndGet();
            return new RateLimitCounterStore.RateLimitDecision(attempt, windowSeconds);
        };
    }
}
