CREATE TABLE activities (
    id              UUID PRIMARY KEY,
    company_id      UUID         NOT NULL REFERENCES companies (id),
    user_id         UUID         NOT NULL REFERENCES users (id),
    client_id       UUID         REFERENCES clients (id),
    lead_id         UUID         REFERENCES leads (id),
    title           VARCHAR(180) NOT NULL,
    description     TEXT,
    type            VARCHAR(32)  NOT NULL,
    occurred_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_activities_type CHECK (type IN ('NOTE', 'CALL', 'EMAIL', 'MEETING', 'OTHER'))
);

CREATE INDEX idx_activities_company_occurred ON activities (company_id, occurred_at DESC);
