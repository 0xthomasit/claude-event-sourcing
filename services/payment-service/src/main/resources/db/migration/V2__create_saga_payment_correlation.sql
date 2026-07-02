CREATE TABLE IF NOT EXISTS saga_payment_correlation (
    payment_id    VARCHAR(36) PRIMARY KEY,
    saga_id       VARCHAR(36) NOT NULL,
    order_id      VARCHAR(36) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_saga_correlation_order
    ON saga_payment_correlation (order_id);
