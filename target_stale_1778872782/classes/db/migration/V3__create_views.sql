-- V3__create_views.sql
-- Analytics views consumed by the Spring AI @Tool beans in nuv-tfg-rs.
-- These are read-only — the backend never writes through these views.

-- ─────────────────────────────────────────────
-- v_inventory_summary
-- Used by: GetInventoryMetricsTool
-- Returns one row per product with current stock status.
-- ─────────────────────────────────────────────
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
        WHEN p.current_quantity = 0               THEN 'OUT_OF_STOCK'
        WHEN p.current_quantity < p.minimum_stock_level THEN 'LOW_STOCK'
        WHEN p.current_quantity > p.minimum_stock_level * 5 THEN 'OVERSTOCK'
        ELSE 'OK'
    END                                                         AS stock_status
FROM products p
JOIN categories c ON c.id = p.category_id;

-- ─────────────────────────────────────────────
-- v_sales_velocity
-- Used by: GetSalesVelocityTool
-- Daily outbound movement per product over the last 30 days.
-- Only counts SALE and DAMAGE (actual stock reductions).
-- ─────────────────────────────────────────────
CREATE OR REPLACE VIEW v_sales_velocity AS
SELECT
    p.id                                            AS product_id,
    p.sku,
    p.name                                          AS product_name,
    c.name                                          AS category_name,
    DATE(sm.moved_at)                               AS movement_date,
    ABS(SUM(sm.delta))                              AS units_sold,
    ROUND(ABS(SUM(sm.delta)) * p.unit_price, 2)    AS revenue
FROM stock_movements sm
JOIN products   p ON p.id = sm.product_id
JOIN categories c ON c.id = p.category_id
WHERE sm.reason IN ('SALE', 'DAMAGE')
  AND sm.moved_at >= NOW() - INTERVAL 30 DAY
GROUP BY p.id, p.sku, p.name, c.name, DATE(sm.moved_at);

-- ─────────────────────────────────────────────
-- v_velocity_summary
-- Used by: GetInventoryMetricsTool (top movers section)
-- Aggregated 7-day and 30-day velocity per product — one row per product.
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
        WHEN sm.moved_at >= NOW() - INTERVAL 7 DAY
         AND sm.reason IN ('SALE', 'DAMAGE')
        THEN ABS(sm.delta) ELSE 0 END), 0)                AS units_sold_7d,
    COALESCE(SUM(CASE
        WHEN sm.moved_at >= NOW() - INTERVAL 30 DAY
         AND sm.reason IN ('SALE', 'DAMAGE')
        THEN ABS(sm.delta) ELSE 0 END), 0)                AS units_sold_30d,
    ROUND(COALESCE(SUM(CASE
        WHEN sm.moved_at >= NOW() - INTERVAL 30 DAY
         AND sm.reason IN ('SALE', 'DAMAGE')
        THEN ABS(sm.delta) ELSE 0 END), 0) / 30, 2)       AS avg_daily_sales,
    -- Estimated days of stock remaining at current sell rate
    CASE
        WHEN COALESCE(SUM(CASE
                WHEN sm.moved_at >= NOW() - INTERVAL 30 DAY
                 AND sm.reason IN ('SALE', 'DAMAGE')
                THEN ABS(sm.delta) ELSE 0 END), 0) = 0
        THEN NULL
        ELSE ROUND(p.current_quantity /
            (COALESCE(SUM(CASE
                WHEN sm.moved_at >= NOW() - INTERVAL 30 DAY
                 AND sm.reason IN ('SALE', 'DAMAGE')
                THEN ABS(sm.delta) ELSE 0 END), 0) / 30), 0)
    END                                                     AS days_of_stock_remaining
FROM products p
JOIN categories c ON c.id = p.category_id
LEFT JOIN stock_movements sm ON sm.product_id = p.id
GROUP BY p.id, p.sku, p.name, c.name, p.unit_price,
         p.current_quantity, p.minimum_stock_level;

-- ─────────────────────────────────────────────
-- v_price_trends
-- Used by: GetPricingHistoryTool
-- Full price change history with context per product.
-- ─────────────────────────────────────────────
CREATE OR REPLACE VIEW v_price_trends AS
SELECT
    ph.id                                                       AS history_id,
    p.id                                                        AS product_id,
    p.sku,
    p.name                                                      AS product_name,
    c.name                                                      AS category_name,
    ph.old_price,
    ph.new_price,
    ROUND(ph.new_price - ph.old_price, 2)                      AS price_delta,
    ROUND(((ph.new_price - ph.old_price) / ph.old_price) * 100, 1) AS pct_change,
    ph.changed_at,
    p.unit_price                                                AS current_price
FROM price_history ph
JOIN products   p ON p.id = ph.product_id
JOIN categories c ON c.id = p.category_id
ORDER BY ph.changed_at DESC;

-- ─────────────────────────────────────────────
-- v_bundle_candidates
-- Used by: GetBundleCandidatesTool
-- Product pairs that were sold within the same 24-hour window,
-- suggesting they are frequently bought together.
-- ─────────────────────────────────────────────
CREATE OR REPLACE VIEW v_bundle_candidates AS
SELECT
    a.product_id                AS product_a_id,
    pa.sku                      AS product_a_sku,
    pa.name                     AS product_a_name,
    pa.unit_price               AS product_a_price,
    b.product_id                AS product_b_id,
    pb.sku                      AS product_b_sku,
    pb.name                     AS product_b_name,
    pb.unit_price               AS product_b_price,
    COUNT(*)                    AS co_occurrence_count,
    ROUND(pa.unit_price + pb.unit_price, 2) AS combined_price
FROM stock_movements a
JOIN stock_movements b
    ON  a.product_id  < b.product_id
    AND a.reason      IN ('SALE')
    AND b.reason      IN ('SALE')
    AND ABS(TIMESTAMPDIFF(HOUR, a.moved_at, b.moved_at)) <= 24
JOIN products pa ON pa.id = a.product_id
JOIN products pb ON pb.id = b.product_id
GROUP BY
    a.product_id, pa.sku, pa.name, pa.unit_price,
    b.product_id, pb.sku, pb.name, pb.unit_price
HAVING COUNT(*) >= 2
ORDER BY co_occurrence_count DESC;
