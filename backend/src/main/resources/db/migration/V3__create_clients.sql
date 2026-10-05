CREATE TABLE clients (
    id              UUID PRIMARY KEY,
    company_id      UUID         NOT NULL REFERENCES companies (id),
    name            VARCHAR(180) NOT NULL,
    email           VARCHAR(180),
    phone           VARCHAR(32),
    address         TEXT,
    city            VARCHAR(100),
    state           VARCHAR(50),
    zip_code        VARCHAR(20),
    country         VARCHAR(100),
    status          VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    notes           TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_clients_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'))
);

CREATE INDEX idx_clients_company_id ON clients (company_id);
CREATE INDEX idx_clients_status ON clients (status);
CREATE INDEX idx_clients_name ON clients (name);
CREATE INDEX idx_clients_email ON clients (email);
