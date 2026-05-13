-- V20__analytics_inventory_timeline.sql
-- Time-series analytics for two questions:
--   1) "When were products created?" (catalog growth over time)
--   2) "When did stock arrive?" (purchase receipts over time)
--
-- Both views are organization-agnostic at the view level; org filtering is
-- applied in the service via the existing organization_id columns on
-- products / stock_movements (same pattern as v_sales_velocity).

-- ─────────────────────────────────────────────
-- v_product_creation_timeline
-- One row per (organization, day) with the count of products created.
-- ─────────────────────────────────────────────
CREATE OR REPLACE VIEW v_product_creation_timeline AS
SELECT
    p.organization_id                          AS organization_id,
    DATE(p.created_at)                         AS creation_date,
    COUNT(*)                                   AS products_created
FROM products p
GROUP BY p.organization_id, DATE(p.created_at);

-- ─────────────────────────────────────────────
-- v_stock_receipts_timeline
-- One row per (organization, day) aggregating PURCHASE stock movements:
--   units_received   = SUM(delta) (purchases are positive deltas)
--   value_received   = SUM(delta * acquisition_price), falling back to
--                      unit_price when acquisition_price is null.
-- Captures both purchase-order receipts (written by
-- PedidoProveedorServiceImpl) and manual stock-ins with reason=PURCHASE.
-- ─────────────────────────────────────────────
CREATE OR REPLACE VIEW v_stock_receipts_timeline AS
SELECT
    sm.organization_id                                                      AS organization_id,
    DATE(sm.moved_at)                                                       AS receipt_date,
    COALESCE(SUM(sm.delta), 0)                                              AS units_received,
    ROUND(COALESCE(SUM(sm.delta * COALESCE(p.acquisition_price, p.unit_price)), 0), 2) AS value_received
FROM stock_movements sm
JOIN products p ON p.id = sm.product_id
WHERE sm.reason = 'PURCHASE'
GROUP BY sm.organization_id, DATE(sm.moved_at);
