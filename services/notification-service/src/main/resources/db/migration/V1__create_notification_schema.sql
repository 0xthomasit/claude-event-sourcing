CREATE TABLE IF NOT EXISTS notification_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id     VARCHAR(36),
    customer_id  VARCHAR(36)  NOT NULL,
    channel      VARCHAR(20)  NOT NULL COMMENT 'EMAIL, SMS, PUSH',
    type         VARCHAR(50)  NOT NULL COMMENT 'ORDER_PLACED, PAYMENT_COMPLETED, ORDER_SHIPPED...',
    status       VARCHAR(20)  NOT NULL COMMENT 'PENDING, SENT, FAILED',
    recipient    VARCHAR(255) NOT NULL COMMENT 'email address or phone number',
    subject      VARCHAR(255),
    body         TEXT,
    sent_at      DATETIME(6),
    error_msg    TEXT,
    retry_count  INT          NOT NULL DEFAULT 0,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_notification_customer ON notification_log (customer_id);
CREATE INDEX idx_notification_order    ON notification_log (order_id);
CREATE INDEX idx_notification_status   ON notification_log (status);
CREATE INDEX idx_notification_created  ON notification_log (created_at);
