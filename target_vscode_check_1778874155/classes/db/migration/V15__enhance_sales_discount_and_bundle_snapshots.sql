-- V15__enhance_sales_discount_and_bundle_snapshots.sql
-- Adds monetary breakdown columns for sales and discount snapshot columns for sale lines.

ALTER TABLE sales
    ADD COLUMN subtotal_amount DECIMAL(12, 2) NULL AFTER notes,
    ADD COLUMN discount_total_amount DECIMAL(12, 2) NULL AFTER subtotal_amount,
    ADD COLUMN global_discount_pct DECIMAL(5, 2) NULL AFTER discount_total_amount;

UPDATE sales
SET subtotal_amount = COALESCE(total_amount, 0),
    discount_total_amount = 0,
    global_discount_pct = 0
WHERE subtotal_amount IS NULL
   OR discount_total_amount IS NULL
   OR global_discount_pct IS NULL;

ALTER TABLE sales
    MODIFY COLUMN subtotal_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    MODIFY COLUMN discount_total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    MODIFY COLUMN global_discount_pct DECIMAL(5, 2) NOT NULL DEFAULT 0;

ALTER TABLE sale_items
    ADD COLUMN list_unit_price_snapshot DECIMAL(10, 2) NULL AFTER quantity,
    ADD COLUMN discount_pct_snapshot DECIMAL(5, 2) NULL AFTER list_unit_price_snapshot;

UPDATE sale_items
SET list_unit_price_snapshot = unit_price_snapshot,
    discount_pct_snapshot = 0
WHERE list_unit_price_snapshot IS NULL
   OR discount_pct_snapshot IS NULL;

ALTER TABLE sale_items
    MODIFY COLUMN list_unit_price_snapshot DECIMAL(10, 2) NOT NULL,
    MODIFY COLUMN discount_pct_snapshot DECIMAL(5, 2) NOT NULL DEFAULT 0;
