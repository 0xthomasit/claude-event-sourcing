CREATE TABLE reviews (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id    VARCHAR(36)  NOT NULL,
    order_id      VARCHAR(36)  NOT NULL,
    customer_id   VARCHAR(36)  NOT NULL,
    rating        SMALLINT     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title         VARCHAR(255),
    content       TEXT         NOT NULL,
    image_urls    JSONB        DEFAULT '[]',
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reject_reason TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (order_id, product_id)
);

CREATE TABLE product_rating_snapshots (
    product_id     VARCHAR(36)  PRIMARY KEY,
    average_rating NUMERIC(3,2) NOT NULL DEFAULT 0,
    total_reviews  INT          NOT NULL DEFAULT 0,
    rating_1       INT          NOT NULL DEFAULT 0,
    rating_2       INT          NOT NULL DEFAULT 0,
    rating_3       INT          NOT NULL DEFAULT 0,
    rating_4       INT          NOT NULL DEFAULT 0,
    rating_5       INT          NOT NULL DEFAULT 0,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE order_delivery_records (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id      VARCHAR(36)  UNIQUE NOT NULL,
    customer_id   VARCHAR(36)  NOT NULL,
    product_ids   JSONB        NOT NULL,
    delivered_at  TIMESTAMPTZ  NOT NULL
);

-- Indexes
CREATE INDEX idx_review_product  ON reviews (product_id, status);
CREATE INDEX idx_review_customer ON reviews (customer_id);
CREATE INDEX idx_review_status   ON reviews (status);
CREATE INDEX idx_review_created  ON reviews (created_at);
CREATE INDEX idx_delivery_cust   ON order_delivery_records (customer_id);
