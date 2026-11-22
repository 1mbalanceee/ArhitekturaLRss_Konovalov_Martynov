BEGIN;

INSERT INTO categories (name, description) VALUES
    ('Кольца', 'Обручальные и декоративные кольца'),
    ('Серьги', 'Парные ювелирные украшения'),
    ('Подвески', 'Подвески для цепочек');

INSERT INTO products (sku, name, metal, fineness, weight_grams, price, stock, category_id)
SELECT 'RING-001', 'Кольцо гладкое', 'Золото', 585, 3.200, 25990.00, 5, id
FROM categories WHERE name = 'Кольца';

INSERT INTO products (sku, name, metal, fineness, weight_grams, price, stock, category_id)
SELECT 'EAR-001', 'Серьги с фианитами', 'Серебро', 925, 4.750, 4990.00, 8, id
FROM categories WHERE name = 'Серьги';

INSERT INTO products (sku, name, metal, fineness, weight_grams, price, stock, category_id)
SELECT 'PEND-001', 'Подвеска круглая', 'Золото', 585, 1.500, 12990.00, 3, id
FROM categories WHERE name = 'Подвески';

COMMIT;
