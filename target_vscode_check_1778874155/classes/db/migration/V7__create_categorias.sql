CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO categories (name) VALUES
    ('Electrónica'),
    ('Alimentación'),
    ('Ropa'),
    ('Hogar'),
    ('Otros');
