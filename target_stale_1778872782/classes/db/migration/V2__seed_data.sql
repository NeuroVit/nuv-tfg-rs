-- V2__seed_data.sql
-- Realistic seed data for local development and AI analysis testing.
-- The stock movement history is intentionally varied so the AI tools
-- have meaningful data to reason about from day one.

-- ─────────────────────────────────────────────
-- Default admin user
-- Password: Admin1234!  (BCrypt hash — change before any real deployment)
-- ─────────────────────────────────────────────
INSERT INTO users (username, email, password_hash, role) VALUES
    ('admin',   'admin@stockmanager.local',   '$2a$12$8eVpSMpEBmIkjvuSFMNTCOGvT5b9TRrQ1BIFz7XtHlKmZ3v9DqPle', 'ROLE_ADMIN'),
    ('manager', 'manager@stockmanager.local', '$2a$12$8eVpSMpEBmIkjvuSFMNTCOGvT5b9TRrQ1BIFz7XtHlKmZ3v9DqPle', 'ROLE_MANAGER'),
    ('viewer',  'viewer@stockmanager.local',  '$2a$12$8eVpSMpEBmIkjvuSFMNTCOGvT5b9TRrQ1BIFz7XtHlKmZ3v9DqPle', 'ROLE_VIEWER');

-- ─────────────────────────────────────────────
-- Categories
-- ─────────────────────────────────────────────
INSERT INTO categories (name, description) VALUES
    ('Electronics',     'Consumer electronics and accessories'),
    ('Office Supplies', 'Stationery, paper, and desk accessories'),
    ('Furniture',       'Office and home furniture'),
    ('Cleaning',        'Cleaning products and supplies'),
    ('Pantry',          'Food, beverages, and kitchen consumables');

-- ─────────────────────────────────────────────
-- Products (30 products across 5 categories)
-- ─────────────────────────────────────────────

-- Electronics (category_id = 1)
INSERT INTO products (name, sku, description, current_quantity, minimum_stock_level, unit_price, category_id) VALUES
    ('USB-C Hub 7-in-1',          'ELEC-001', '7-port USB-C hub with HDMI, SD card reader, and 100W PD', 45,  10, 34.99,  1),
    ('Wireless Keyboard',         'ELEC-002', 'Compact bluetooth keyboard, multi-device, rechargeable',   30,  8,  49.99,  1),
    ('Wireless Mouse',            'ELEC-003', 'Ergonomic wireless mouse, 1600 DPI, silent clicks',        28,  8,  29.99,  1),
    ('27" Monitor Stand',         'ELEC-004', 'Adjustable monitor arm for screens up to 32"',             12,  5,  59.99,  1),
    ('Webcam 1080p',              'ELEC-005', 'Full HD webcam with built-in noise-cancelling microphone',  7,  10, 79.99,  1),
    ('Mechanical Keyboard',       'ELEC-006', 'TKL mechanical keyboard, brown switches, RGB backlight',   3,  5,  89.99,  1),
    ('USB-C Cable 2m',            'ELEC-007', '2m braided USB-C to USB-C cable, 100W, 10Gbps',           120, 20, 12.99,  1),
    ('Laptop Cooling Pad',        'ELEC-008', 'Dual-fan cooling pad for laptops up to 17"',               18,  8,  24.99,  1);

-- Office Supplies (category_id = 2)
INSERT INTO products (name, sku, description, current_quantity, minimum_stock_level, unit_price, category_id) VALUES
    ('A4 Paper Ream 500 sheets',  'OFF-001', '80g/m² white copier paper, box of 5 reams',               200, 50, 8.99,   2),
    ('Ballpoint Pens Box 50',     'OFF-002', 'Blue ballpoint pens, medium tip, box of 50',               85,  20, 7.49,   2),
    ('Sticky Notes Pack',         'OFF-003', '76x76mm sticky notes, 6 colours, 100 sheets each',         60,  15, 5.99,   2),
    ('Stapler Heavy Duty',        'OFF-004', 'Heavy duty stapler, up to 100 sheets',                      9,  5,  19.99,  2),
    ('Staples Box 5000',          'OFF-005', 'Standard 26/6 staples, box of 5000',                       40,  10, 3.49,   2),
    ('Binder A4 Lever Arch',      'OFF-006', 'A4 lever arch file, 75mm spine, assorted colours',         55,  15, 4.99,   2),
    ('Whiteboard Markers Set',    'OFF-007', 'Dry-erase markers, 4 colours, low-odour',                  22,  10, 8.99,   2),
    ('Scissors 21cm',             'OFF-008', 'Stainless steel office scissors, comfort grip',              8,  10, 6.49,   2);

-- Furniture (category_id = 3)
INSERT INTO products (name, sku, description, current_quantity, minimum_stock_level, unit_price, category_id) VALUES
    ('Ergonomic Office Chair',    'FURN-001', 'Mesh back, lumbar support, adjustable armrests',            4,  2,  249.99, 3),
    ('Standing Desk 140x70cm',    'FURN-002', 'Electric height-adjustable desk, memory presets',           2,  1,  449.99, 3),
    ('3-Drawer Pedestal',         'FURN-003', 'Mobile under-desk pedestal with lock, A4 filing',           6,  2,  119.99, 3),
    ('Bookcase 5-Shelf',          'FURN-004', 'Freestanding bookcase, 180cm tall, white',                  3,  1,  89.99,  3),
    ('Desk Lamp LED',             'FURN-005', 'LED desk lamp, 3 colour temps, wireless charging base',    11,  4,  44.99,  3);

-- Cleaning (category_id = 4)
INSERT INTO products (name, sku, description, current_quantity, minimum_stock_level, unit_price, category_id) VALUES
    ('Multi-Surface Spray 1L',   'CLN-001', 'Anti-bacterial multi-surface cleaner, citrus scent',         90,  20, 3.99,   4),
    ('Microfibre Cloths Pack 10', 'CLN-002', 'Pack of 10 microfibre cleaning cloths, 40x40cm',            50,  15, 9.99,   4),
    ('Hand Soap Refill 5L',      'CLN-003', 'Moisturising liquid hand soap refill, fragrance-free',       30,  10, 14.99,  4),
    ('Bin Liners 80L Box 50',    'CLN-004', 'Heavy duty black bin liners, 80L, box of 50',               110,  25, 11.99,  4),
    ('Floor Mop Flat',           'CLN-005', 'Flat microfibre mop with telescopic handle, washable head',   5,  3,  29.99,  4);

-- Pantry (category_id = 5)
INSERT INTO products (name, sku, description, current_quantity, minimum_stock_level, unit_price, category_id) VALUES
    ('Ground Coffee 1kg',         'PNT-001', 'Medium roast ground coffee, Arabica blend',                 25,  8,  18.99,  5),
    ('Tea Bags Box 240',          'PNT-002', 'Everyday black tea bags, individually wrapped',             40,  10, 7.99,   5),
    ('Sugar White 1kg',           'PNT-003', 'Granulated white sugar, 1kg bag',                           18,  5,  1.99,   5),
    ('Oat Milk 1L x6',            'PNT-004', 'Barista oat milk, 1L carton, pack of 6',                   14,  6,  11.99,  5),
    ('Sparkling Water 500ml x24', 'PNT-005', 'Still mineral water, 500ml PET, case of 24',               8,  12, 9.99,   5),
    ('Instant Noodles Box 24',    'PNT-006', 'Chicken flavour instant noodles, box of 24',               35,  10, 14.99,  5);

-- ─────────────────────────────────────────────
-- Stock movements — 90 days of varied history
-- Mix of sales (negative) and purchases (positive).
-- The AI velocity analysis reads this table.
-- ─────────────────────────────────────────────

-- USB-C Hub: steady seller, recently increased demand
INSERT INTO stock_movements (product_id, delta, reason, moved_at) VALUES
    (1, 60,  'INITIAL',   NOW() - INTERVAL 90 DAY),
    (1, -4,  'SALE',      NOW() - INTERVAL 80 DAY),
    (1, -3,  'SALE',      NOW() - INTERVAL 70 DAY),
    (1, 30,  'PURCHASE',  NOW() - INTERVAL 65 DAY),
    (1, -5,  'SALE',      NOW() - INTERVAL 60 DAY),
    (1, -6,  'SALE',      NOW() - INTERVAL 50 DAY),
    (1, -4,  'SALE',      NOW() - INTERVAL 40 DAY),
    (1, -8,  'SALE',      NOW() - INTERVAL 30 DAY),
    (1, 30,  'PURCHASE',  NOW() - INTERVAL 25 DAY),
    (1, -7,  'SALE',      NOW() - INTERVAL 15 DAY),
    (1, -8,  'SALE',      NOW() - INTERVAL 7 DAY),
    (1, -9,  'SALE',      NOW() - INTERVAL 2 DAY);

-- Wireless Keyboard: consistent mid-volume
INSERT INTO stock_movements (product_id, delta, reason, moved_at) VALUES
    (2, 40,  'INITIAL',   NOW() - INTERVAL 90 DAY),
    (2, -3,  'SALE',      NOW() - INTERVAL 75 DAY),
    (2, -2,  'SALE',      NOW() - INTERVAL 60 DAY),
    (2, 20,  'PURCHASE',  NOW() - INTERVAL 55 DAY),
    (2, -4,  'SALE',      NOW() - INTERVAL 45 DAY),
    (2, -3,  'SALE',      NOW() - INTERVAL 30 DAY),
    (2, -4,  'SALE',      NOW() - INTERVAL 15 DAY),
    (2, -5,  'SALE',      NOW() - INTERVAL 5 DAY),
    (2,  9,  'PURCHASE',  NOW() - INTERVAL 2 DAY);

-- Webcam 1080p: low stock, slow-moving but high value
INSERT INTO stock_movements (product_id, delta, reason, moved_at) VALUES
    (5, 20,  'INITIAL',   NOW() - INTERVAL 90 DAY),
    (5, -2,  'SALE',      NOW() - INTERVAL 80 DAY),
    (5, -3,  'SALE',      NOW() - INTERVAL 60 DAY),
    (5, -2,  'SALE',      NOW() - INTERVAL 45 DAY),
    (5, -4,  'SALE',      NOW() - INTERVAL 30 DAY),
    (5, -2,  'SALE',      NOW() - INTERVAL 10 DAY);

-- Mechanical Keyboard: critically low, fast-moving recently
INSERT INTO stock_movements (product_id, delta, reason, moved_at) VALUES
    (6, 20,  'INITIAL',   NOW() - INTERVAL 90 DAY),
    (6, -2,  'SALE',      NOW() - INTERVAL 70 DAY),
    (6, -3,  'SALE',      NOW() - INTERVAL 55 DAY),
    (6, -4,  'SALE',      NOW() - INTERVAL 40 DAY),
    (6, -3,  'SALE',      NOW() - INTERVAL 25 DAY),
    (6, -2,  'SALE',      NOW() - INTERVAL 15 DAY),
    (6, -3,  'SALE',      NOW() - INTERVAL 5 DAY);

-- A4 Paper: high-volume, frequent restocks
INSERT INTO stock_movements (product_id, delta, reason, moved_at) VALUES
    (9, 300, 'INITIAL',   NOW() - INTERVAL 90 DAY),
    (9, -40, 'SALE',      NOW() - INTERVAL 80 DAY),
    (9, -35, 'SALE',      NOW() - INTERVAL 70 DAY),
    (9, 200, 'PURCHASE',  NOW() - INTERVAL 65 DAY),
    (9, -30, 'SALE',      NOW() - INTERVAL 60 DAY),
    (9, -45, 'SALE',      NOW() - INTERVAL 50 DAY),
    (9, -40, 'SALE',      NOW() - INTERVAL 40 DAY),
    (9, 150, 'PURCHASE',  NOW() - INTERVAL 35 DAY),
    (9, -35, 'SALE',      NOW() - INTERVAL 25 DAY),
    (9, -30, 'SALE',      NOW() - INTERVAL 15 DAY),
    (9, -45, 'SALE',      NOW() - INTERVAL 5 DAY);

-- Ergonomic Chair: slow-moving, high value
INSERT INTO stock_movements (product_id, delta, reason, moved_at) VALUES
    (17, 10, 'INITIAL',   NOW() - INTERVAL 90 DAY),
    (17, -2, 'SALE',      NOW() - INTERVAL 75 DAY),
    (17, -2, 'SALE',      NOW() - INTERVAL 50 DAY),
    (17, -2, 'SALE',      NOW() - INTERVAL 30 DAY);

-- Ground Coffee: very fast-moving pantry item
INSERT INTO stock_movements (product_id, delta, reason, moved_at) VALUES
    (25, 50, 'INITIAL',   NOW() - INTERVAL 90 DAY),
    (25, -8, 'SALE',      NOW() - INTERVAL 80 DAY),
    (25, -7, 'SALE',      NOW() - INTERVAL 70 DAY),
    (25, 40, 'PURCHASE',  NOW() - INTERVAL 65 DAY),
    (25, -9, 'SALE',      NOW() - INTERVAL 55 DAY),
    (25, -8, 'SALE',      NOW() - INTERVAL 45 DAY),
    (25, -7, 'SALE',      NOW() - INTERVAL 35 DAY),
    (25, 30, 'PURCHASE',  NOW() - INTERVAL 30 DAY),
    (25, -8, 'SALE',      NOW() - INTERVAL 20 DAY),
    (25, -8, 'SALE',      NOW() - INTERVAL 10 DAY),
    (25, -8, 'SALE',      NOW() - INTERVAL 3 DAY);

-- Sparkling Water: currently below minimum stock
INSERT INTO stock_movements (product_id, delta, reason, moved_at) VALUES
    (29, 40, 'INITIAL',   NOW() - INTERVAL 90 DAY),
    (29, -6, 'SALE',      NOW() - INTERVAL 75 DAY),
    (29, -5, 'SALE',      NOW() - INTERVAL 60 DAY),
    (29, -7, 'SALE',      NOW() - INTERVAL 45 DAY),
    (29, -6, 'SALE',      NOW() - INTERVAL 30 DAY),
    (29, -5, 'SALE',      NOW() - INTERVAL 15 DAY),
    (29, -3, 'SALE',      NOW() - INTERVAL 5 DAY);

-- USB-C Cable: very high volume, commodity item
INSERT INTO stock_movements (product_id, delta, reason, moved_at) VALUES
    (7, 200, 'INITIAL',   NOW() - INTERVAL 90 DAY),
    (7, -20, 'SALE',      NOW() - INTERVAL 80 DAY),
    (7, -18, 'SALE',      NOW() - INTERVAL 70 DAY),
    (7, 100, 'PURCHASE',  NOW() - INTERVAL 65 DAY),
    (7, -22, 'SALE',      NOW() - INTERVAL 60 DAY),
    (7, -20, 'SALE',      NOW() - INTERVAL 50 DAY),
    (7, -15, 'SALE',      NOW() - INTERVAL 40 DAY),
    (7, 80,  'PURCHASE',  NOW() - INTERVAL 35 DAY),
    (7, -18, 'SALE',      NOW() - INTERVAL 25 DAY),
    (7, -22, 'SALE',      NOW() - INTERVAL 15 DAY),
    (7, -25, 'SALE',      NOW() - INTERVAL 5 DAY);

-- ─────────────────────────────────────────────
-- Price history — some fluctuation for AI to analyse
-- ─────────────────────────────────────────────
INSERT INTO price_history (product_id, old_price, new_price, changed_by, changed_at) VALUES
    -- USB-C Hub: one price increase
    (1,  29.99, 34.99, 1, NOW() - INTERVAL 45 DAY),
    -- Mechanical Keyboard: price reduced to clear stock, then raised
    (6, 99.99, 79.99, 1, NOW() - INTERVAL 60 DAY),
    (6, 79.99, 89.99, 1, NOW() - INTERVAL 20 DAY),
    -- Ground Coffee: two increments over time
    (25, 15.99, 17.49, 1, NOW() - INTERVAL 70 DAY),
    (25, 17.49, 18.99, 1, NOW() - INTERVAL 35 DAY),
    -- A4 Paper: supplier increase passed on
    (9,  7.99,  8.99, 1, NOW() - INTERVAL 50 DAY),
    -- Webcam: slight reduction to stimulate sales
    (5, 89.99, 79.99, 1, NOW() - INTERVAL 25 DAY);

-- ─────────────────────────────────────────────
-- Bundles — two pre-defined deals
-- ─────────────────────────────────────────────
INSERT INTO bundles (name, description, discount_pct) VALUES
    ('Home Office Starter',   'Keyboard, mouse, and USB-C hub bundle',            10.00),
    ('Barista Kit',           'Ground coffee, oat milk, and tea bags bundle',      8.00);

-- Bundle items
INSERT INTO bundle_items (bundle_id, product_id, quantity) VALUES
    (1, 2, 1),   -- Wireless Keyboard
    (1, 3, 1),   -- Wireless Mouse
    (1, 1, 1),   -- USB-C Hub
    (2, 25, 1),  -- Ground Coffee
    (2, 27, 1),  -- Oat Milk
    (2, 26, 1);  -- Tea Bags
