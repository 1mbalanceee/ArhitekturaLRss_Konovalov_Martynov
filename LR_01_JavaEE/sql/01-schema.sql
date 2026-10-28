BEGIN;

CREATE TABLE categories (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(100) NOT NULL CHECK (length(btrim(name)) > 0),
    description VARCHAR(500)
);
CREATE UNIQUE INDEX uq_categories_name ON categories (lower(name));

CREATE TABLE products (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    sku VARCHAR(40) NOT NULL UNIQUE CHECK (sku ~ '^[A-Z0-9-]{1,40}$'),
    name VARCHAR(150) NOT NULL CHECK (length(btrim(name)) > 0),
    metal VARCHAR(50) NOT NULL CHECK (length(btrim(metal)) > 0),
    fineness INTEGER NOT NULL CHECK (fineness BETWEEN 1 AND 999),
    weight_grams NUMERIC(10, 3) NOT NULL CHECK (weight_grams > 0),
    price NUMERIC(12, 2) NOT NULL CHECK (price > 0),
    stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    category_id BIGINT NOT NULL REFERENCES categories (id) ON DELETE RESTRICT
);
CREATE INDEX idx_products_category ON products (category_id);

COMMIT;
