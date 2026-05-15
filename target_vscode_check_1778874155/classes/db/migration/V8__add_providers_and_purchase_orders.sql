CREATE TABLE providers (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    name                VARCHAR(200)    NOT NULL,
    warehouse_location  VARCHAR(500)    NOT NULL,
    lead_time_days      INT             NOT NULL,
    is_active           TINYINT(1)      NOT NULL DEFAULT 1,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at          DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_providers_name (name),
    CHECK (lead_time_days >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE products
    ADD COLUMN provider_id BIGINT NULL AFTER category_id;

INSERT INTO providers (name, warehouse_location, lead_time_days, is_active)
SELECT 'Sin proveedor', 'Sin ubicación definida', 0, 1
WHERE NOT EXISTS (
    SELECT 1 FROM providers WHERE name = 'Sin proveedor'
);

UPDATE products
SET provider_id = (
    SELECT id FROM providers WHERE name = 'Sin proveedor' LIMIT 1
)
WHERE provider_id IS NULL;

ALTER TABLE products
    MODIFY COLUMN provider_id BIGINT NOT NULL;

ALTER TABLE products
    ADD CONSTRAINT fk_products_provider
        FOREIGN KEY (provider_id) REFERENCES providers (id)
        ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE products
    ADD INDEX idx_products_provider (provider_id);

CREATE TABLE purchase_orders (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    provider_id             BIGINT          NOT NULL,
    status                  ENUM('DRAFT', 'SENT', 'RECEIVED', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    desired_arrival_date    DATE            NOT NULL,
    recommended_order_date  DATE            NOT NULL,
    sent_at                 DATETIME,
    received_at             DATETIME,
    notes                   VARCHAR(500),
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_purchase_orders_provider
        FOREIGN KEY (provider_id) REFERENCES providers (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_purchase_orders_provider (provider_id),
    INDEX idx_purchase_orders_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE purchase_order_items (
    order_id              BIGINT          NOT NULL,
    product_id            BIGINT          NOT NULL,
    quantity              INT             NOT NULL,
    unit_price_snapshot   DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (order_id, product_id),
    CONSTRAINT fk_purchase_order_items_order
        FOREIGN KEY (order_id) REFERENCES purchase_orders (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_purchase_order_items_product
        FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
