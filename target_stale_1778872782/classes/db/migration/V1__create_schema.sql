-- V1__create_schema.sql
-- Full schema for the Intelligent Stock Manager.
-- Managed by Flyway — never edit this file after it has been applied.
-- To change the schema, create a new V4__... migration.

SET FOREIGN_KEY_CHECKS = 0;

-- ─────────────────────────────────────────────
-- Users
-- ─────────────────────────────────────────────
CREATE TABLE users (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)     NOT NULL,
    email         VARCHAR(255)    NOT NULL,
    password_hash VARCHAR(255)    NOT NULL,
    role          ENUM(
                    'ROLE_ADMIN',
                    'ROLE_MANAGER',
                    'ROLE_VIEWER'
                  )               NOT NULL DEFAULT 'ROLE_VIEWER',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                           ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_email    (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─────────────────────────────────────────────
-- Categories
-- ─────────────────────────────────────────────
CREATE TABLE categories (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)    NOT NULL,
    description VARCHAR(500),
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_categories_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─────────────────────────────────────────────
-- Products
-- ─────────────────────────────────────────────
CREATE TABLE products (
    id                   BIGINT           NOT NULL AUTO_INCREMENT,
    name                 VARCHAR(200)     NOT NULL,
    sku                  VARCHAR(100)     NOT NULL,
    description          TEXT,
    current_quantity     INT              NOT NULL DEFAULT 0,
    minimum_stock_level  INT              NOT NULL DEFAULT 10,
    unit_price           DECIMAL(10, 2)   NOT NULL,
    category_id          BIGINT           NOT NULL,
    -- Optimistic locking — incremented by JPA on every update
    version              BIGINT           NOT NULL DEFAULT 0,
    created_at           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                   ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_products_sku (sku),
    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES categories (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_products_category  (category_id),
    INDEX idx_products_quantity  (current_quantity),
    INDEX idx_products_updated   (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─────────────────────────────────────────────
-- Stock movements
-- Every quantity change is recorded here.
-- This is the primary dataset for AI sales-velocity analysis.
-- ─────────────────────────────────────────────
CREATE TABLE stock_movements (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    product_id  BIGINT          NOT NULL,
    -- Positive = stock in, negative = stock out
    delta       INT             NOT NULL,
    reason      ENUM(
                  'SALE',
                  'PURCHASE',
                  'ADJUSTMENT',
                  'RETURN',
                  'DAMAGE',
                  'INITIAL'
                )               NOT NULL,
    notes       VARCHAR(500),
    moved_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_movements_product
        FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_movements_product     (product_id),
    INDEX idx_movements_moved_at    (moved_at),
    INDEX idx_movements_product_date (product_id, moved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─────────────────────────────────────────────
-- Price history
-- Every price change is recorded here.
-- Primary dataset for AI pricing-recommendation analysis.
-- ─────────────────────────────────────────────
CREATE TABLE price_history (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    product_id  BIGINT          NOT NULL,
    old_price   DECIMAL(10, 2)  NOT NULL,
    new_price   DECIMAL(10, 2)  NOT NULL,
    changed_by  BIGINT,
    changed_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_price_history_product
        FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_price_history_user
        FOREIGN KEY (changed_by) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX idx_price_history_product      (product_id),
    INDEX idx_price_history_changed_at   (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─────────────────────────────────────────────
-- Bundles
-- ─────────────────────────────────────────────
CREATE TABLE bundles (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    name         VARCHAR(200)    NOT NULL,
    description  VARCHAR(500),
    discount_pct DECIMAL(5, 2)   NOT NULL DEFAULT 0.00
                                 CHECK (discount_pct BETWEEN 0 AND 100),
    active       TINYINT(1)      NOT NULL DEFAULT 1,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                          ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bundle_items (
    bundle_id   BIGINT  NOT NULL,
    product_id  BIGINT  NOT NULL,
    quantity    INT     NOT NULL DEFAULT 1,
    PRIMARY KEY (bundle_id, product_id),
    CONSTRAINT fk_bundle_items_bundle
        FOREIGN KEY (bundle_id) REFERENCES bundles (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_bundle_items_product
        FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─────────────────────────────────────────────
-- Stock alerts
-- Generated automatically by the backend scheduler.
-- ─────────────────────────────────────────────
CREATE TABLE stock_alerts (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    product_id  BIGINT          NOT NULL,
    alert_type  ENUM(
                  'LOW_STOCK',
                  'OUT_OF_STOCK',
                  'OVERSTOCK',
                  'PRICE_ANOMALY'
                )               NOT NULL,
    message     VARCHAR(500)    NOT NULL,
    resolved    TINYINT(1)      NOT NULL DEFAULT 0,
    resolved_at DATETIME,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_alerts_product
        FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_alerts_resolved   (resolved),
    INDEX idx_alerts_product    (product_id),
    INDEX idx_alerts_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
