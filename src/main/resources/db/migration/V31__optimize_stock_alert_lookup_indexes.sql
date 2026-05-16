-- V31__optimize_stock_alert_lookup_indexes.sql
-- Speeds up alert lookups executed during stock changes (sales/purchases/product updates).

CREATE INDEX idx_stock_alerts_org_product_resolved_type
    ON stock_alerts (organization_id, product_id, resolved, alert_type);

CREATE INDEX idx_stock_alerts_org_product_resolved
    ON stock_alerts (organization_id, product_id, resolved);
