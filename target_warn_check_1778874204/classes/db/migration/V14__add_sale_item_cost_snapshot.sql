-- V14__add_sale_item_cost_snapshot.sql
-- Store real cost at sale line creation to calculate exact profitability.

ALTER TABLE sale_items
    ADD COLUMN cost_price_snapshot DECIMAL(10, 2) NULL AFTER unit_price_snapshot;

UPDATE sale_items si
JOIN products p ON p.id = si.product_id
SET si.cost_price_snapshot = COALESCE(p.acquisition_price, p.unit_price)
WHERE si.cost_price_snapshot IS NULL;

ALTER TABLE sale_items
    MODIFY COLUMN cost_price_snapshot DECIMAL(10, 2) NOT NULL;
