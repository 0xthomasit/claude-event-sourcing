-- Order Reservations tracking table
-- Allows efficient lookup of "which products were reserved for a given orderId"
-- Required for handleByOrderId() in ReleaseStockHandler and ReduceStockHandler

CREATE TABLE IF NOT EXISTS order_reservations (
    id              BIGSERIAL    PRIMARY KEY,
    order_id        VARCHAR(36)  NOT NULL,
    aggregate_id    VARCHAR(36)  NOT NULL,   -- StockItem aggregate ID
    product_id      VARCHAR(36)  NOT NULL,
    quantity        INT          NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'RESERVED',  -- RESERVED, RELEASED, REDUCED
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_reservations_order_id   ON order_reservations (order_id);
CREATE INDEX idx_order_reservations_product_id ON order_reservations (product_id);
CREATE INDEX idx_order_reservations_status     ON order_reservations (order_id, status);
