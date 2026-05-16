ALTER TABLE users
    ADD COLUMN time_zone VARCHAR(64) NOT NULL DEFAULT 'Europe/Madrid' AFTER token_version;

UPDATE products
SET created_at = CONVERT_TZ(created_at, 'Europe/Madrid', '+00:00'),
    updated_at = CONVERT_TZ(updated_at, 'Europe/Madrid', '+00:00');

UPDATE stock_movements
SET moved_at = CONVERT_TZ(moved_at, 'Europe/Madrid', '+00:00');

UPDATE sales
SET sold_at = CONVERT_TZ(sold_at, 'Europe/Madrid', '+00:00'),
    created_at = CONVERT_TZ(created_at, 'Europe/Madrid', '+00:00');

SET @sale_items_has_created_at := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sale_items'
      AND column_name = 'created_at'
);
SET @sale_items_sql := IF(
    @sale_items_has_created_at > 0,
    'UPDATE sale_items SET created_at = CONVERT_TZ(created_at, ''Europe/Madrid'', ''+00:00'')',
    'SELECT 1'
);
PREPARE stmt_sale_items FROM @sale_items_sql;
EXECUTE stmt_sale_items;
DEALLOCATE PREPARE stmt_sale_items;

UPDATE price_history
SET changed_at = CONVERT_TZ(changed_at, 'Europe/Madrid', '+00:00');

UPDATE purchase_orders
SET created_at = CONVERT_TZ(created_at, 'Europe/Madrid', '+00:00'),
    updated_at = CONVERT_TZ(updated_at, 'Europe/Madrid', '+00:00'),
    sent_at = CONVERT_TZ(sent_at, 'Europe/Madrid', '+00:00'),
    received_at = CONVERT_TZ(received_at, 'Europe/Madrid', '+00:00');

SET @po_items_has_created_at := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'purchase_order_items'
      AND column_name = 'created_at'
);
SET @po_items_sql := IF(
    @po_items_has_created_at > 0,
    'UPDATE purchase_order_items SET created_at = CONVERT_TZ(created_at, ''Europe/Madrid'', ''+00:00'')',
    'SELECT 1'
);
PREPARE stmt_po_items FROM @po_items_sql;
EXECUTE stmt_po_items;
DEALLOCATE PREPARE stmt_po_items;

UPDATE stock_alerts
SET created_at = CONVERT_TZ(created_at, 'Europe/Madrid', '+00:00'),
    resolved_at = CONVERT_TZ(resolved_at, 'Europe/Madrid', '+00:00');

UPDATE clients
SET created_at = CONVERT_TZ(created_at, 'Europe/Madrid', '+00:00'),
    updated_at = CONVERT_TZ(updated_at, 'Europe/Madrid', '+00:00');

UPDATE users
SET created_at = CONVERT_TZ(created_at, 'Europe/Madrid', '+00:00'),
    updated_at = CONVERT_TZ(updated_at, 'Europe/Madrid', '+00:00');

UPDATE categories
SET created_at = CONVERT_TZ(created_at, 'Europe/Madrid', '+00:00');

UPDATE providers
SET created_at = CONVERT_TZ(created_at, 'Europe/Madrid', '+00:00'),
    updated_at = CONVERT_TZ(updated_at, 'Europe/Madrid', '+00:00'),
    deleted_at = CONVERT_TZ(deleted_at, 'Europe/Madrid', '+00:00');

UPDATE bundles
SET created_at = CONVERT_TZ(created_at, 'Europe/Madrid', '+00:00'),
    updated_at = CONVERT_TZ(updated_at, 'Europe/Madrid', '+00:00');

UPDATE organizations
SET created_at = CONVERT_TZ(created_at, 'Europe/Madrid', '+00:00'),
    updated_at = CONVERT_TZ(updated_at, 'Europe/Madrid', '+00:00');

UPDATE organization_memberships
SET created_at = CONVERT_TZ(created_at, 'Europe/Madrid', '+00:00'),
    updated_at = CONVERT_TZ(updated_at, 'Europe/Madrid', '+00:00');

UPDATE user_role_audit
SET changed_at = CONVERT_TZ(changed_at, 'Europe/Madrid', '+00:00');
