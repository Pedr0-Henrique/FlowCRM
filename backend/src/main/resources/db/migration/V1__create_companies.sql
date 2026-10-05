CREATE TABLE companies (
    id              UUID PRIMARY KEY,
    name            VARCHAR(180) NOT NULL,
    slug            VARCHAR(80)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_companies_slug UNIQUE (slug)
);
