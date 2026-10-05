package com.flowcrm.shared.security;

public interface RateLimitCounterStore {

    RateLimitDecision consume(String clientAddress, String endpoint, int limit, long windowSeconds);

    record RateLimitDecision(int attempts, long retryAfterSeconds) {
    }
}
