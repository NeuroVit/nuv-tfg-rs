CREATE TABLE clients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(40),
    notes VARCHAR(500),
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_clients_email (email),
    INDEX idx_clients_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sales (
    id BIGINT NOT NULL AUTO_INCREMENT,
    client_id BIGINT NOT NULL,
    sold_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(500),
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_sales_client
        FOREIGN KEY (client_id) REFERENCES clients (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_sales_created_by
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX idx_sales_client (client_id),
    INDEX idx_sales_sold_at (sold_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sale_items (
    sale_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price_snapshot DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (sale_id, product_id),
    CONSTRAINT fk_sale_items_sale
        FOREIGN KEY (sale_id) REFERENCES sales (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_sale_items_product
        FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
