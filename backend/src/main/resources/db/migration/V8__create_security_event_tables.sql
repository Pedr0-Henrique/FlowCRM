CREATE TABLE auth_rate_limit_counters (
    counter_key CHAR(64) PRIMARY KEY,
    window_started_at TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL CHECK (attempts > 0),
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_auth_rate_limit_updated_at
    ON auth_rate_limit_counters (updated_at);

CREATE TABLE audit_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    http_method VARCHAR(10) NOT NULL,
    request_path TEXT NOT NULL,
    response_status INTEGER NOT NULL,
    user_id UUID,
    company_id UUID,
    remote_addr VARCHAR(45) NOT NULL
);

CREATE INDEX idx_audit_events_occurred_at
    ON audit_events (occurred_at);

CREATE INDEX idx_audit_events_company_occurred_at
    ON audit_events (company_id, occurred_at);
