-- V17__add_organizations_multitenancy.sql
-- MVP single-database multitenancy using organization_id tenant scoping.

CREATE TABLE organizations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_organizations_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE organization_memberships (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role ENUM('OWNER', 'ADMIN', 'MEMBER') NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_org_memberships_user (user_id),
    UNIQUE KEY uq_org_memberships_org_user (organization_id, user_id),
    INDEX idx_org_memberships_org (organization_id),
    CONSTRAINT fk_org_memberships_org
        FOREIGN KEY (organization_id) REFERENCES organizations(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_org_memberships_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO organizations (name, slug)
SELECT 'Default Organization', 'default'
WHERE NOT EXISTS (
    SELECT 1 FROM organizations WHERE slug = 'default'
);

SET @default_org_id := (SELECT id FROM organizations WHERE slug = 'default' LIMIT 1);

ALTER TABLE categories ADD COLUMN organization_id BIGINT NULL AFTER id;
ALTER TABLE providers ADD COLUMN organization_id BIGINT NULL AFTER id;
ALTER TABLE products ADD COLUMN organization_id BIGINT NULL AFTER id;
ALTER TABLE stock_movements ADD COLUMN organization_id BIGINT NULL AFTER id;
ALTER TABLE price_history ADD COLUMN organization_id BIGINT NULL AFTER id;
ALTER TABLE bundles ADD COLUMN organization_id BIGINT NULL AFTER id;
ALTER TABLE bundle_items ADD COLUMN organization_id BIGINT NULL AFTER bundle_id;
ALTER TABLE stock_alerts ADD COLUMN organization_id BIGINT NULL AFTER id;
ALTER TABLE clients ADD COLUMN organization_id BIGINT NULL AFTER id;
ALTER TABLE sales ADD COLUMN organization_id BIGINT NULL AFTER id;
ALTER TABLE sale_items ADD COLUMN organization_id BIGINT NULL AFTER sale_id;
ALTER TABLE purchase_orders ADD COLUMN organization_id BIGINT NULL AFTER id;
ALTER TABLE purchase_order_items ADD COLUMN organization_id BIGINT NULL AFTER order_id;

UPDATE categories SET organization_id = @default_org_id WHERE organization_id IS NULL;
UPDATE providers SET organization_id = @default_org_id WHERE organization_id IS NULL;
UPDATE products SET organization_id = @default_org_id WHERE organization_id IS NULL;
UPDATE stock_movements sm
JOIN products p ON p.id = sm.product_id
SET sm.organization_id = p.organization_id
WHERE sm.organization_id IS NULL;
UPDATE price_history ph
JOIN products p ON p.id = ph.product_id
SET ph.organization_id = p.organization_id
WHERE ph.organization_id IS NULL;
UPDATE bundles SET organization_id = @default_org_id WHERE organization_id IS NULL;
UPDATE bundle_items bi
JOIN bundles b ON b.id = bi.bundle_id
SET bi.organization_id = b.organization_id
WHERE bi.organization_id IS NULL;
UPDATE stock_alerts sa
JOIN products p ON p.id = sa.product_id
SET sa.organization_id = p.organization_id
WHERE sa.organization_id IS NULL;
UPDATE clients SET organization_id = @default_org_id WHERE organization_id IS NULL;
UPDATE sales s
JOIN clients c ON c.id = s.client_id
SET s.organization_id = c.organization_id
WHERE s.organization_id IS NULL;
UPDATE sale_items si
JOIN sales s ON s.id = si.sale_id
SET si.organization_id = s.organization_id
WHERE si.organization_id IS NULL;
UPDATE purchase_orders po
JOIN providers p ON p.id = po.provider_id
SET po.organization_id = p.organization_id
WHERE po.organization_id IS NULL;
UPDATE purchase_order_items poi
JOIN purchase_orders po ON po.id = poi.order_id
SET poi.organization_id = po.organization_id
WHERE poi.organization_id IS NULL;

ALTER TABLE categories MODIFY COLUMN organization_id BIGINT NOT NULL;
ALTER TABLE providers MODIFY COLUMN organization_id BIGINT NOT NULL;
ALTER TABLE products MODIFY COLUMN organization_id BIGINT NOT NULL;
ALTER TABLE stock_movements MODIFY COLUMN organization_id BIGINT NOT NULL;
ALTER TABLE price_history MODIFY COLUMN organization_id BIGINT NOT NULL;
ALTER TABLE bundles MODIFY COLUMN organization_id BIGINT NOT NULL;
ALTER TABLE bundle_items MODIFY COLUMN organization_id BIGINT NOT NULL;
ALTER TABLE stock_alerts MODIFY COLUMN organization_id BIGINT NOT NULL;
ALTER TABLE clients MODIFY COLUMN organization_id BIGINT NOT NULL;
ALTER TABLE sales MODIFY COLUMN organization_id BIGINT NOT NULL;
ALTER TABLE sale_items MODIFY COLUMN organization_id BIGINT NOT NULL;
ALTER TABLE purchase_orders MODIFY COLUMN organization_id BIGINT NOT NULL;
ALTER TABLE purchase_order_items MODIFY COLUMN organization_id BIGINT NOT NULL;

ALTER TABLE categories
    DROP INDEX uq_categories_name,
    ADD UNIQUE KEY uq_categories_org_name (organization_id, name),
    ADD INDEX idx_categories_org (organization_id),
    ADD CONSTRAINT fk_categories_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE providers
    DROP INDEX uq_providers_name,
    ADD UNIQUE KEY uq_providers_org_name (organization_id, name),
    ADD INDEX idx_providers_org (organization_id),
    ADD CONSTRAINT fk_providers_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE products
    DROP INDEX uq_products_sku,
    ADD UNIQUE KEY uq_products_org_sku (organization_id, sku),
    ADD INDEX idx_products_org (organization_id),
    ADD CONSTRAINT fk_products_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE stock_movements
    ADD INDEX idx_stock_movements_org (organization_id),
    ADD CONSTRAINT fk_stock_movements_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE price_history
    ADD INDEX idx_price_history_org (organization_id),
    ADD CONSTRAINT fk_price_history_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE bundles
    ADD INDEX idx_bundles_org (organization_id),
    ADD CONSTRAINT fk_bundles_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE bundle_items
    ADD INDEX idx_bundle_items_org (organization_id),
    ADD CONSTRAINT fk_bundle_items_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE stock_alerts
    ADD INDEX idx_stock_alerts_org (organization_id),
    ADD CONSTRAINT fk_stock_alerts_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE clients
    DROP INDEX uq_clients_email,
    ADD UNIQUE KEY uq_clients_org_email (organization_id, email),
    ADD INDEX idx_clients_org (organization_id),
    ADD CONSTRAINT fk_clients_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE sales
    ADD INDEX idx_sales_org (organization_id),
    ADD CONSTRAINT fk_sales_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE sale_items
    ADD INDEX idx_sale_items_org (organization_id),
    ADD CONSTRAINT fk_sale_items_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE purchase_orders
    ADD INDEX idx_purchase_orders_org (organization_id),
    ADD CONSTRAINT fk_purchase_orders_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE purchase_order_items
    ADD INDEX idx_purchase_order_items_org (organization_id),
    ADD CONSTRAINT fk_purchase_order_items_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT ON UPDATE CASCADE;

INSERT INTO organization_memberships (organization_id, user_id, role)
SELECT
    @default_org_id,
    u.id,
    CASE
        WHEN u.role = 'ROLE_ADMIN' THEN 'OWNER'
        WHEN u.role = 'ROLE_MANAGER' THEN 'ADMIN'
        ELSE 'MEMBER'
    END
FROM users u
WHERE NOT EXISTS (
    SELECT 1
    FROM organization_memberships om
    WHERE om.user_id = u.id
);
