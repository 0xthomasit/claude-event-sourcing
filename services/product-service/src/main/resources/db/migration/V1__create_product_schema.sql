CREATE TABLE IF NOT EXISTS categories (
    id        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name      VARCHAR(100) NOT NULL,
    slug      VARCHAR(100) NOT NULL UNIQUE,
    parent_id UUID         REFERENCES categories(id)
);

CREATE INDEX IF NOT EXISTS idx_categories_parent_id ON categories(parent_id);
CREATE INDEX IF NOT EXISTS idx_categories_slug      ON categories(slug);

CREATE TABLE IF NOT EXISTS products (
    id           UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    sku          VARCHAR(50)    NOT NULL UNIQUE,
    name         VARCHAR(255)   NOT NULL,
    description  TEXT,
    price        DECIMAL(15, 2) NOT NULL,
    currency     VARCHAR(3)     NOT NULL DEFAULT 'VND',
    category_id  UUID           REFERENCES categories(id),
    status       VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    image_url    VARCHAR(500),
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_products_sku         ON products(sku);
CREATE INDEX IF NOT EXISTS idx_products_status      ON products(status);
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);