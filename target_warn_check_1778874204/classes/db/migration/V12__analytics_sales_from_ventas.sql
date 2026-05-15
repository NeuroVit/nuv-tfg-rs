-- V12__analytics_sales_from_ventas.sql
-- Align sales analytics with real business sales data (sales + sale_items),
-- instead of stock movements.

-- ─────────────────────────────────────────────
-- v_sales_velocity
-- Daily sold units and revenue per product from real sales.
-- ─────────────────────────────────────────────
CREATE OR REPLACE VIEW v_sales_velocity AS
SELECT
    p.id                                         AS product_id,
    p.sku,
    p.name                                       AS product_name,
    c.name                                       AS category_name,
    DATE(s.sold_at)                              AS movement_date,
    SUM(si.quantity)                             AS units_sold,
    ROUND(SUM(si.quantity * si.unit_price_snapshot), 2) AS revenue
FROM sales s
JOIN sale_items si ON si.sale_id = s.id
JOIN products p    ON p.id = si.product_id
JOIN categories c  ON c.id = p.category_id
GROUP BY p.id, p.sku, p.name, c.name, DATE(s.sold_at);

-- ─────────────────────────────────────────────
-- v_velocity_summary
-- 7d/30d velocity from real sales.
-- ─────────────────────────────────────────────
CREATE OR REPLACE VIEW v_velocity_summary AS
SELECT
    p.id                                                    AS product_id,
    p.sku,
    p.name                                                  AS product_name,
    c.name                                                  AS category_name,
    p.unit_price,
    p.current_quantity,
    p.minimum_stock_level,
    COALESCE(SUM(CASE
        WHEN s.sold_at >= NOW() - INTERVAL 7 DAY
        THEN si.quantity ELSE 0 END), 0)                  AS units_sold_7d,
    COALESCE(SUM(CASE
        WHEN s.sold_at >= NOW() - INTERVAL 30 DAY
        THEN si.quantity ELSE 0 END), 0)                  AS units_sold_30d,
    ROUND(
        COALESCE(SUM(CASE
            WHEN s.sold_at >= NOW() - INTERVAL 30 DAY
            THEN si.quantity ELSE 0 END), 0) / 30, 2
    )                                                       AS avg_daily_sales,
    CASE
        WHEN COALESCE(SUM(CASE
            WHEN s.sold_at >= NOW() - INTERVAL 30 DAY
            THEN si.quantity ELSE 0 END), 0) = 0
        THEN NULL
        ELSE ROUND(
            p.current_quantity / (
                COALESCE(SUM(CASE
                    WHEN s.sold_at >= NOW() - INTERVAL 30 DAY
                    THEN si.quantity ELSE 0 END), 0
                ) / 30
            ),
            0
        )
    END                                                     AS days_of_stock_remaining
FROM products p
JOIN categories c ON c.id = p.category_id
LEFT JOIN sale_items si ON si.product_id = p.id
LEFT JOIN sales s ON s.id = si.sale_id
GROUP BY p.id, p.sku, p.name, c.name, p.unit_price, p.current_quantity, p.minimum_stock_level;

-- ─────────────────────────────────────────────
-- v_bundle_candidates
-- Pair co-occurrence from products sold in the same sale.
-- ─────────────────────────────────────────────
CREATE OR REPLACE VIEW v_bundle_candidates AS
SELECT
    a.product_id                                    AS product_a_id,
    pa.sku                                          AS product_a_sku,
    pa.name                                         AS product_a_name,
    pa.unit_price                                   AS product_a_price,
    b.product_id                                    AS product_b_id,
    pb.sku                                          AS product_b_sku,
    pb.name                                         AS product_b_name,
    pb.unit_price                                   AS product_b_price,
    COUNT(DISTINCT a.sale_id)                       AS co_occurrence_count,
    ROUND(pa.unit_price + pb.unit_price, 2)         AS combined_price
FROM sale_items a
JOIN sale_items b
    ON a.sale_id = b.sale_id
   AND a.product_id < b.product_id
JOIN products pa ON pa.id = a.product_id
JOIN products pb ON pb.id = b.product_id
GROUP BY
    a.product_id, pa.sku, pa.name, pa.unit_price,
    b.product_id, pb.sku, pb.name, pb.unit_price
HAVING COUNT(DISTINCT a.sale_id) >= 2
ORDER BY co_occurrence_count DESC;
