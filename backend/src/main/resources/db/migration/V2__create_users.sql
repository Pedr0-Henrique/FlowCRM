CREATE TABLE users (
    id              UUID PRIMARY KEY,
    company_id      UUID         NOT NULL REFERENCES companies (id),
    name            VARCHAR(160) NOT NULL,
    email           VARCHAR(180) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(32)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_company_email UNIQUE (company_id, email),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'MANAGER', 'SALES', 'USER'))
);

CREATE INDEX idx_users_company_id ON users (company_id);
