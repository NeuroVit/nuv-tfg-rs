-- V22__add_bundle_id_to_sale_items.sql
-- Keep bundle attribution at sale line level for bundle analytics.

ALTER TABLE sale_items
    ADD COLUMN bundle_id BIGINT NULL AFTER product_id,
    ADD CONSTRAINT fk_sale_items_bundle
        FOREIGN KEY (bundle_id) REFERENCES bundles(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE;

CREATE INDEX idx_sale_items_bundle_id ON sale_items(bundle_id);
