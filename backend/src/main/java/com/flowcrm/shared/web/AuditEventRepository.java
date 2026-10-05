package com.flowcrm.shared.web;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class AuditEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuditEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(
            String method,
            String path,
            int status,
            UUID userId,
            UUID companyId,
            String remoteAddress
    ) {
        jdbcTemplate.update(
                """
                        INSERT INTO audit_events
                            (http_method, request_path, response_status, user_id, company_id, remote_addr)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                method,
                path,
                status,
                userId,
                companyId,
                remoteAddress
        );
    }
}
