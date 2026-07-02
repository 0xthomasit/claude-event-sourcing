CREATE TABLE invoices (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number   VARCHAR(30)  UNIQUE NOT NULL,
    order_id         VARCHAR(36)  UNIQUE NOT NULL,
    customer_id      VARCHAR(36)  NOT NULL,
    customer_name    VARCHAR(255),
    customer_email   VARCHAR(255),
    subtotal         NUMERIC(15,2) NOT NULL,
    discount_amount  NUMERIC(15,2) DEFAULT 0,
    tax_rate         NUMERIC(5,4) NOT NULL DEFAULT 0.0800,
    tax_amount       NUMERIC(15,2) NOT NULL,
    total_amount     NUMERIC(15,2) NOT NULL,
    currency         VARCHAR(3)   NOT NULL DEFAULT 'VND',
    status           VARCHAR(20)  NOT NULL DEFAULT 'ISSUED',
    void_reason      TEXT,
    e_invoice_ref    VARCHAR(100),
    issued_at        TIMESTAMPTZ,
    voided_at        TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE invoice_line_items (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id    UUID         NOT NULL REFERENCES invoices(id),
    product_id    VARCHAR(36)  NOT NULL,
    product_name  VARCHAR(255) NOT NULL,
    sku           VARCHAR(50),
    quantity      INT          NOT NULL,
    unit_price    NUMERIC(15,2) NOT NULL,
    line_total    NUMERIC(15,2) NOT NULL
);

-- Indexes
CREATE INDEX idx_invoice_order    ON invoices (order_id);
CREATE INDEX idx_invoice_customer ON invoices (customer_id);
CREATE INDEX idx_invoice_issued   ON invoices (issued_at);
CREATE INDEX idx_invoice_number   ON invoices (invoice_number);
CREATE INDEX idx_line_item_inv    ON invoice_line_items (invoice_id);
