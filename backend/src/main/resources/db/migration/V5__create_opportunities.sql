CREATE TABLE opportunities (
    id              UUID PRIMARY KEY,
    company_id      UUID         NOT NULL REFERENCES companies (id),
    client_id       UUID         REFERENCES clients (id),
    lead_id         UUID         REFERENCES leads (id),
    assigned_to_id  UUID         REFERENCES users (id),
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    value           DECIMAL(15, 2) NOT NULL,
    stage           VARCHAR(32)  NOT NULL DEFAULT 'NEW',
    probability     INTEGER      NOT NULL DEFAULT 10,
    expected_close_date DATE,
    actual_close_date DATE,
    notes           TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_opportunities_stage CHECK (stage IN ('NEW', 'CONTACT', 'PROPOSAL', 'NEGOTIATION', 'CLOSED')),
    CONSTRAINT ck_opportunities_probability CHECK (probability >= 0 AND probability <= 100)
);

CREATE INDEX idx_opportunities_company_id ON opportunities (company_id);
CREATE INDEX idx_opportunities_client_id ON opportunities (client_id);
CREATE INDEX idx_opportunities_lead_id ON opportunities (lead_id);
CREATE INDEX idx_opportunities_assigned_to_id ON opportunities (assigned_to_id);
CREATE INDEX idx_opportunities_stage ON opportunities (stage);
CREATE INDEX idx_opportunities_expected_close_date ON opportunities (expected_close_date);
