CREATE TABLE promotions (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code                VARCHAR(50)  UNIQUE NOT NULL,
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    promotion_type      VARCHAR(20)  NOT NULL,
    discount_type       VARCHAR(20)  NOT NULL,
    discount_value      NUMERIC(15,2) NOT NULL,
    min_order_amount    NUMERIC(15,2) DEFAULT 0,
    max_discount_amount NUMERIC(15,2),
    total_quantity      INT          NOT NULL,
    used_quantity       INT          NOT NULL DEFAULT 0,
    max_per_customer    INT          DEFAULT 1,
    start_date          TIMESTAMPTZ  NOT NULL,
    end_date            TIMESTAMPTZ  NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE promotion_redemptions (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    promotion_id      UUID         NOT NULL REFERENCES promotions(id),
    promotion_code    VARCHAR(50)  NOT NULL,
    order_id          VARCHAR(36)  NOT NULL,
    customer_id       VARCHAR(36)  NOT NULL,
    discount_applied  NUMERIC(15,2) NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'APPLIED',
    redeemed_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    released_at       TIMESTAMPTZ,
    confirmed_at      TIMESTAMPTZ
);

-- Indexes
CREATE INDEX idx_promo_code       ON promotions (code);
CREATE INDEX idx_promo_status     ON promotions (status, start_date, end_date);
CREATE INDEX idx_redemption_order ON promotion_redemptions (order_id);
CREATE INDEX idx_redemption_cust  ON promotion_redemptions (customer_id, promotion_id);
CREATE INDEX idx_redemption_promo ON promotion_redemptions (promotion_id, status);
