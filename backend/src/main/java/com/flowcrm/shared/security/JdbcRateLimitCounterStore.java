package com.flowcrm.shared.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

@Repository
public class JdbcRateLimitCounterStore implements RateLimitCounterStore {

    private static final String CONSUME_SQL = """
            INSERT INTO auth_rate_limit_counters (counter_key, window_started_at, attempts, updated_at)
            VALUES (?, clock_timestamp(), 1, clock_timestamp())
            ON CONFLICT (counter_key) DO UPDATE SET
                window_started_at = CASE
                    WHEN auth_rate_limit_counters.window_started_at
                        <= EXCLUDED.updated_at - (? * INTERVAL '1 second')
                    THEN EXCLUDED.updated_at
                    ELSE auth_rate_limit_counters.window_started_at
                END,
                attempts = CASE
                    WHEN auth_rate_limit_counters.window_started_at
                        <= EXCLUDED.updated_at - (? * INTERVAL '1 second')
                    THEN 1
                    ELSE LEAST(auth_rate_limit_counters.attempts + 1, ? + 1)
                END,
                updated_at = EXCLUDED.updated_at
            RETURNING attempts,
                GREATEST(1, CEIL(EXTRACT(EPOCH FROM (
                    window_started_at + (? * INTERVAL '1 second') - updated_at
                ))))::BIGINT AS retry_after_seconds
            """;

    private final JdbcTemplate jdbcTemplate;
    private final String hashSecret;

    public JdbcRateLimitCounterStore(
            JdbcTemplate jdbcTemplate,
            @Value("${app.security.rate-limit.hash-secret:${app.jwt.secret}}") String hashSecret
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.hashSecret = hashSecret;
    }

    @Override
    public RateLimitDecision consume(String clientAddress, String endpoint, int limit, long windowSeconds) {
        String key = hash(clientAddress + ":" + endpoint);
        return jdbcTemplate.queryForObject(
                CONSUME_SQL,
                (resultSet, rowNum) -> new RateLimitDecision(
                        resultSet.getInt("attempts"),
                        resultSet.getLong("retry_after_seconds")
                ),
                key,
                windowSeconds,
                windowSeconds,
                limit,
                windowSeconds
        );
    }

    private String hash(String value) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(hmac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }
}
