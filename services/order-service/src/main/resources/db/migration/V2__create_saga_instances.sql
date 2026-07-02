CREATE TABLE IF NOT EXISTS saga_instances (
    id              UUID         PRIMARY KEY,
    saga_type       VARCHAR(100) NOT NULL,
    order_id        VARCHAR(36)  NOT NULL,
    current_step    VARCHAR(50)  NOT NULL,
    payload         JSONB        NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ,
    failure_reason  TEXT
);

CREATE INDEX IF NOT EXISTS idx_saga_order_id ON saga_instances (order_id);
CREATE INDEX IF NOT EXISTS idx_saga_step     ON saga_instances (current_step);
CREATE INDEX IF NOT EXISTS idx_saga_updated  ON saga_instances (updated_at)
    WHERE completed_at IS NULL;
