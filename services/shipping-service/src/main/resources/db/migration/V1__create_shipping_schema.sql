CREATE TABLE IF NOT EXISTS shipments (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    order_id        VARCHAR(36)  NOT NULL UNIQUE,
    customer_id     VARCHAR(36)  NOT NULL,
    carrier         VARCHAR(20)  NOT NULL COMMENT 'GHN, GHTK, MANUAL',
    tracking_code   VARCHAR(100),
    status          VARCHAR(30)  NOT NULL COMMENT 'PENDING, PICKED_UP, IN_TRANSIT, DELIVERED, FAILED',
    recipient_name  VARCHAR(255) NOT NULL,
    recipient_phone VARCHAR(20)  NOT NULL,
    address         TEXT         NOT NULL,
    district        VARCHAR(100) NOT NULL,
    province        VARCHAR(100) NOT NULL,
    estimated_at    DATETIME(6),
    delivered_at    DATETIME(6),
    failed_reason   TEXT,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS shipment_tracking_events (
    id           BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    shipment_id  VARCHAR(36) NOT NULL,
    status       VARCHAR(30) NOT NULL,
    location     VARCHAR(255),
    description  TEXT,
    occurred_at  DATETIME(6) NOT NULL,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (shipment_id) REFERENCES shipments(id)
);

CREATE INDEX idx_shipment_order    ON shipments (order_id);
CREATE INDEX idx_shipment_customer ON shipments (customer_id);
CREATE INDEX idx_shipment_status   ON shipments (status);
CREATE INDEX idx_tracking_shipment ON shipment_tracking_events (shipment_id);
