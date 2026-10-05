CREATE TABLE leads (
    id              UUID PRIMARY KEY,
    company_id      UUID         NOT NULL REFERENCES companies (id),
    assigned_to_id  UUID         REFERENCES users (id),
    name            VARCHAR(180) NOT NULL,
    email           VARCHAR(180),
    phone           VARCHAR(32),
    source          VARCHAR(50),
    status          VARCHAR(32)  NOT NULL DEFAULT 'NEW',
    priority        VARCHAR(32)  NOT NULL DEFAULT 'MEDIUM',
    estimated_value DECIMAL(15, 2),
    notes           TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_leads_status CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'PROPOSAL', 'NEGOTIATION', 'WON', 'LOST')),
    CONSTRAINT ck_leads_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT ck_leads_source CHECK (source IN ('WEBSITE', 'REFERRAL', 'SOCIAL_MEDIA', 'EMAIL', 'PHONE', 'EVENT', 'ADVERTISEMENT', 'OTHER') OR source IS NULL)
);

CREATE INDEX idx_leads_company_id ON leads (company_id);
CREATE INDEX idx_leads_assigned_to_id ON leads (assigned_to_id);
CREATE INDEX idx_leads_status ON leads (status);
CREATE INDEX idx_leads_priority ON leads (priority);
CREATE INDEX idx_leads_name ON leads (name);
CREATE INDEX idx_leads_email ON leads (email);
