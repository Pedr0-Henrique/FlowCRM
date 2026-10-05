CREATE TABLE tasks (
    id              UUID PRIMARY KEY,
    company_id      UUID         NOT NULL REFERENCES companies (id),
    assigned_to_id  UUID         REFERENCES users (id),
    client_id       UUID         REFERENCES clients (id),
    lead_id         UUID         REFERENCES leads (id),
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    status          VARCHAR(32)  NOT NULL DEFAULT 'TODO',
    priority        VARCHAR(32)  NOT NULL DEFAULT 'MEDIUM',
    due_date        TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_tasks_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT'))
);

CREATE INDEX idx_tasks_company_id ON tasks (company_id);
CREATE INDEX idx_tasks_assigned_to_id ON tasks (assigned_to_id);
CREATE INDEX idx_tasks_client_id ON tasks (client_id);
CREATE INDEX idx_tasks_lead_id ON tasks (lead_id);
CREATE INDEX idx_tasks_status ON tasks (status);
CREATE INDEX idx_tasks_priority ON tasks (priority);
CREATE INDEX idx_tasks_due_date ON tasks (due_date);
