ALTER TABLE products
    ADD COLUMN maximum_stock_level INT NULL AFTER minimum_stock_level;

UPDATE products
SET maximum_stock_level = CASE
    WHEN minimum_stock_level <= 0 THEN GREATEST(current_quantity, 1)
    ELSE GREATEST(minimum_stock_level * 5, minimum_stock_level)
END
WHERE maximum_stock_level IS NULL;

ALTER TABLE products
    MODIFY COLUMN maximum_stock_level INT NOT NULL DEFAULT 100000,
    ADD INDEX idx_products_maximum_stock_level (maximum_stock_level);

CREATE OR REPLACE VIEW v_inventory_summary AS
SELECT
    p.id                                                        AS product_id,
    p.sku,
    p.name                                                      AS product_name,
    c.name                                                      AS category_name,
    p.current_quantity,
    p.minimum_stock_level,
    p.unit_price,
    ROUND(p.current_quantity * p.unit_price, 2)                AS stock_value,
    CASE
        WHEN p.current_quantity = 0 THEN 'OUT_OF_STOCK'
        WHEN p.current_quantity < p.minimum_stock_level THEN 'LOW_STOCK'
        WHEN p.current_quantity > p.maximum_stock_level THEN 'OVERSTOCK'
        ELSE 'OK'
    END                                                         AS stock_status
FROM products p
JOIN categories c ON c.id = p.category_id;
