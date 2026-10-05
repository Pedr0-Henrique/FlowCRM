package com.flowcrm.shared.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SecurityDataRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(SecurityDataRetentionJob.class);

    private final JdbcTemplate jdbcTemplate;
    private final int auditRetentionDays;

    public SecurityDataRetentionJob(
            JdbcTemplate jdbcTemplate,
            @Value("${app.security.retention.audit-days:90}") int auditRetentionDays
    ) {
        if (auditRetentionDays < 1) {
            throw new IllegalArgumentException("Audit retention must be positive");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.auditRetentionDays = auditRetentionDays;
    }

    @Scheduled(cron = "${app.security.retention.cleanup-cron:0 0 3 * * *}")
    public void cleanExpiredSecurityData() {
        int expiredRateLimitCounters = jdbcTemplate.update(
                "DELETE FROM auth_rate_limit_counters WHERE updated_at < clock_timestamp() - INTERVAL '1 day'"
        );
        int expiredAuditEvents = jdbcTemplate.update(
                "DELETE FROM audit_events WHERE occurred_at < clock_timestamp() - (? * INTERVAL '1 day')",
                auditRetentionDays
        );
        log.info(
                "event=security_data_retention_complete rateLimitCountersDeleted={} auditEventsDeleted={} auditRetentionDays={}",
                expiredRateLimitCounters,
                expiredAuditEvents,
                auditRetentionDays
        );
    }
}
