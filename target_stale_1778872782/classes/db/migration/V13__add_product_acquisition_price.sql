-- V13__add_product_acquisition_price.sql
-- Adds acquisition cost basis per product for business-finance analytics.

ALTER TABLE products
    ADD COLUMN acquisition_price DECIMAL(10, 2) NULL AFTER unit_price;

-- Backfill deterministic mock acquisition costs for existing products.
UPDATE products
SET acquisition_price = ROUND(unit_price * (0.58 + ((id % 7) * 0.04)), 2)
WHERE acquisition_price IS NULL;
