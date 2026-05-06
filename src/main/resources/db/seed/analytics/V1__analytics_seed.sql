-- App-wide mock seed (idempotent).
-- Executed when analytics.seed.enabled=true.

-- 1) Master data
INSERT INTO providers (name, warehouse_location, lead_time_days, is_active)
SELECT 'Proveedor Mock Demo', 'Madrid - Zona Demo [MOCK]', 4, 1
WHERE NOT EXISTS (SELECT 1 FROM providers WHERE name = 'Proveedor Mock Demo');

INSERT INTO categories (name, description)
SELECT 'Electronics', '[MOCK] Consumer electronics demo data.'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Electronics');

INSERT INTO categories (name, description)
SELECT 'Office Supplies', '[MOCK] Office supplies demo data.'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Office Supplies');

INSERT INTO categories (name, description)
SELECT 'Pantry', '[MOCK] Pantry demo data.'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Pantry');

INSERT INTO categories (name, description)
SELECT 'Mock Data Lab', '[MOCK] Dedicated category for testing negative margins.'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Mock Data Lab');

INSERT INTO products (name, sku, description, current_quantity, minimum_stock_level, unit_price, acquisition_price, category_id, provider_id)
SELECT 'USB-C Hub 7-in-1', 'ELEC-001', '[MOCK] Bestseller USB-C hub.', 45, 10, 34.99, 23.10, c.id, p.id
FROM categories c
JOIN providers p ON p.name = 'Proveedor Mock Demo'
WHERE c.name = 'Electronics'
  AND NOT EXISTS (SELECT 1 FROM products WHERE sku = 'ELEC-001');

INSERT INTO products (name, sku, description, current_quantity, minimum_stock_level, unit_price, acquisition_price, category_id, provider_id)
SELECT 'Wireless Keyboard', 'ELEC-002', '[MOCK] Wireless keyboard.', 30, 8, 49.99, 30.49, c.id, p.id
FROM categories c
JOIN providers p ON p.name = 'Proveedor Mock Demo'
WHERE c.name = 'Electronics'
  AND NOT EXISTS (SELECT 1 FROM products WHERE sku = 'ELEC-002');

INSERT INTO products (name, sku, description, current_quantity, minimum_stock_level, unit_price, acquisition_price, category_id, provider_id)
SELECT 'Wireless Mouse', 'ELEC-003', '[MOCK] Wireless mouse.', 28, 8, 29.99, 19.19, c.id, p.id
FROM categories c
JOIN providers p ON p.name = 'Proveedor Mock Demo'
WHERE c.name = 'Electronics'
  AND NOT EXISTS (SELECT 1 FROM products WHERE sku = 'ELEC-003');

INSERT INTO products (name, sku, description, current_quantity, minimum_stock_level, unit_price, acquisition_price, category_id, provider_id)
SELECT 'Webcam 1080p', 'ELEC-005', '[MOCK] Webcam stock test.', 7, 10, 79.99, 53.59, c.id, p.id
FROM categories c
JOIN providers p ON p.name = 'Proveedor Mock Demo'
WHERE c.name = 'Electronics'
  AND NOT EXISTS (SELECT 1 FROM products WHERE sku = 'ELEC-005');

INSERT INTO products (name, sku, description, current_quantity, minimum_stock_level, unit_price, acquisition_price, category_id, provider_id)
SELECT 'Mechanical Keyboard', 'ELEC-006', '[MOCK] Product used for loser margins.', 3, 5, 89.99, 58.49, c.id, p.id
FROM categories c
JOIN providers p ON p.name = 'Proveedor Mock Demo'
WHERE c.name = 'Electronics'
  AND NOT EXISTS (SELECT 1 FROM products WHERE sku = 'ELEC-006');

INSERT INTO products (name, sku, description, current_quantity, minimum_stock_level, unit_price, acquisition_price, category_id, provider_id)
SELECT 'USB-C Cable 2m', 'ELEC-007', '[MOCK] High-rotation cable.', 120, 20, 12.99, 8.45, c.id, p.id
FROM categories c
JOIN providers p ON p.name = 'Proveedor Mock Demo'
WHERE c.name = 'Electronics'
  AND NOT EXISTS (SELECT 1 FROM products WHERE sku = 'ELEC-007');

INSERT INTO products (name, sku, description, current_quantity, minimum_stock_level, unit_price, acquisition_price, category_id, provider_id)
SELECT 'A4 Paper Ream 500 sheets', 'OFF-001', '[MOCK] Office volume product.', 200, 50, 8.99, 5.48, c.id, p.id
FROM categories c
JOIN providers p ON p.name = 'Proveedor Mock Demo'
WHERE c.name = 'Office Supplies'
  AND NOT EXISTS (SELECT 1 FROM products WHERE sku = 'OFF-001');

INSERT INTO products (name, sku, description, current_quantity, minimum_stock_level, unit_price, acquisition_price, category_id, provider_id)
SELECT 'Ground Coffee 1kg', 'PNT-001', '[MOCK] Pantry fast seller.', 25, 8, 18.99, 11.77, c.id, p.id
FROM categories c
JOIN providers p ON p.name = 'Proveedor Mock Demo'
WHERE c.name = 'Pantry'
  AND NOT EXISTS (SELECT 1 FROM products WHERE sku = 'PNT-001');

INSERT INTO products (name, sku, description, current_quantity, minimum_stock_level, unit_price, acquisition_price, category_id, provider_id)
SELECT 'Producto Demo Perdedor', 'MOCK-LOSS-001', '[MOCK] Intentional negative-margin product.', 32, 10, 14.99, 18.90, c.id, p.id
FROM categories c
JOIN providers p ON p.name = 'Proveedor Mock Demo'
WHERE c.name = 'Mock Data Lab'
  AND NOT EXISTS (SELECT 1 FROM products WHERE sku = 'MOCK-LOSS-001');

INSERT INTO bundles (name, description, discount_pct, active)
SELECT 'Bundle Home Office Mock', '[MOCK] Bundle for app-wide demo.', 8.00, 1
WHERE NOT EXISTS (SELECT 1 FROM bundles WHERE name = 'Bundle Home Office Mock');

INSERT INTO bundles (name, description, discount_pct, active)
SELECT 'Bundle Mock Rentabilidad', '[MOCK] Bundle with loser SKU for analytics.', 6.50, 1
WHERE NOT EXISTS (SELECT 1 FROM bundles WHERE name = 'Bundle Mock Rentabilidad');

INSERT INTO bundle_items (bundle_id, product_id, quantity)
SELECT b.id, p.id, 1
FROM bundles b
JOIN products p ON p.sku = 'ELEC-002'
WHERE b.name = 'Bundle Home Office Mock'
  AND NOT EXISTS (SELECT 1 FROM bundle_items bi WHERE bi.bundle_id = b.id AND bi.product_id = p.id);

INSERT INTO bundle_items (bundle_id, product_id, quantity)
SELECT b.id, p.id, 1
FROM bundles b
JOIN products p ON p.sku = 'ELEC-003'
WHERE b.name = 'Bundle Home Office Mock'
  AND NOT EXISTS (SELECT 1 FROM bundle_items bi WHERE bi.bundle_id = b.id AND bi.product_id = p.id);

INSERT INTO bundle_items (bundle_id, product_id, quantity)
SELECT b.id, p.id, 1
FROM bundles b
JOIN products p ON p.sku = 'ELEC-001'
WHERE b.name = 'Bundle Home Office Mock'
  AND NOT EXISTS (SELECT 1 FROM bundle_items bi WHERE bi.bundle_id = b.id AND bi.product_id = p.id);

INSERT INTO bundle_items (bundle_id, product_id, quantity)
SELECT b.id, p.id, 1
FROM bundles b
JOIN products p ON p.sku = 'MOCK-LOSS-001'
WHERE b.name = 'Bundle Mock Rentabilidad'
  AND NOT EXISTS (SELECT 1 FROM bundle_items bi WHERE bi.bundle_id = b.id AND bi.product_id = p.id);

-- 2) Clients
INSERT INTO clients (name, email, phone, notes, is_active, created_at, updated_at)
SELECT 'Analytics Client Uno', 'analytics.client1@nuv.local', '600100001', '[ANALYTICS_SEED] client-1', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM clients WHERE email = 'analytics.client1@nuv.local');

INSERT INTO clients (name, email, phone, notes, is_active, created_at, updated_at)
SELECT 'Analytics Client Dos', 'analytics.client2@nuv.local', '600100002', '[ANALYTICS_SEED] client-2', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM clients WHERE email = 'analytics.client2@nuv.local');

INSERT INTO clients (name, email, phone, notes, is_active, created_at, updated_at)
SELECT 'Analytics Client Tres', 'analytics.client3@nuv.local', '600100003', '[ANALYTICS_SEED] client-3', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM clients WHERE email = 'analytics.client3@nuv.local');

-- 3) Sales (with explicit loser product)
INSERT INTO sales (client_id, sold_at, notes, subtotal_amount, discount_total_amount, global_discount_pct, total_amount, created_by, created_at)
SELECT c.id, NOW() - INTERVAL 10 DAY, '[ANALYTICS_SEED] SALE-001', 295.68, 5.68, 1.92, 290.00, 1, NOW() - INTERVAL 10 DAY
FROM clients c
WHERE c.email = 'analytics.client1@nuv.local'
  AND NOT EXISTS (SELECT 1 FROM sales s WHERE s.notes = '[ANALYTICS_SEED] SALE-001');

INSERT INTO sales (client_id, sold_at, notes, subtotal_amount, discount_total_amount, global_discount_pct, total_amount, created_by, created_at)
SELECT c.id, NOW() - INTERVAL 5 DAY, '[ANALYTICS_SEED] SALE-002', 164.41, 4.41, 2.68, 160.00, 1, NOW() - INTERVAL 5 DAY
FROM clients c
WHERE c.email = 'analytics.client2@nuv.local'
  AND NOT EXISTS (SELECT 1 FROM sales s WHERE s.notes = '[ANALYTICS_SEED] SALE-002');

INSERT INTO sales (client_id, sold_at, notes, subtotal_amount, discount_total_amount, global_discount_pct, total_amount, created_by, created_at)
SELECT c.id, NOW() - INTERVAL 2 DAY, '[ANALYTICS_SEED] SALE-003', 156.44, 0.00, 0.00, 156.44, 1, NOW() - INTERVAL 2 DAY
FROM clients c
WHERE c.email = 'analytics.client3@nuv.local'
  AND NOT EXISTS (SELECT 1 FROM sales s WHERE s.notes = '[ANALYTICS_SEED] SALE-003');

INSERT INTO sale_items (sale_id, product_id, quantity, list_unit_price_snapshot, discount_pct_snapshot, unit_price_snapshot, cost_price_snapshot)
SELECT s.id, p.id, 3, 34.99, 2.00, 34.29, 23.10
FROM sales s
JOIN products p ON p.sku = 'ELEC-001'
WHERE s.notes = '[ANALYTICS_SEED] SALE-001'
  AND NOT EXISTS (SELECT 1 FROM sale_items si WHERE si.sale_id = s.id AND si.product_id = p.id);

INSERT INTO sale_items (sale_id, product_id, quantity, list_unit_price_snapshot, discount_pct_snapshot, unit_price_snapshot, cost_price_snapshot)
SELECT s.id, p.id, 8, 12.99, 0.00, 12.99, 8.45
FROM sales s
JOIN products p ON p.sku = 'ELEC-007'
WHERE s.notes = '[ANALYTICS_SEED] SALE-001'
  AND NOT EXISTS (SELECT 1 FROM sale_items si WHERE si.sale_id = s.id AND si.product_id = p.id);

INSERT INTO sale_items (sale_id, product_id, quantity, list_unit_price_snapshot, discount_pct_snapshot, unit_price_snapshot, cost_price_snapshot)
SELECT s.id, p.id, 10, 8.99, 2.50, 8.77, 5.48
FROM sales s
JOIN products p ON p.sku = 'OFF-001'
WHERE s.notes = '[ANALYTICS_SEED] SALE-001'
  AND NOT EXISTS (SELECT 1 FROM sale_items si WHERE si.sale_id = s.id AND si.product_id = p.id);

INSERT INTO sale_items (sale_id, product_id, quantity, list_unit_price_snapshot, discount_pct_snapshot, unit_price_snapshot, cost_price_snapshot)
SELECT s.id, p.id, 2, 49.99, 7.00, 46.49, 30.49
FROM sales s
JOIN products p ON p.sku = 'ELEC-002'
WHERE s.notes = '[ANALYTICS_SEED] SALE-002'
  AND NOT EXISTS (SELECT 1 FROM sale_items si WHERE si.sale_id = s.id AND si.product_id = p.id);

INSERT INTO sale_items (sale_id, product_id, quantity, list_unit_price_snapshot, discount_pct_snapshot, unit_price_snapshot, cost_price_snapshot)
SELECT s.id, p.id, 2, 29.99, 5.00, 28.49, 19.19
FROM sales s
JOIN products p ON p.sku = 'ELEC-003'
WHERE s.notes = '[ANALYTICS_SEED] SALE-002'
  AND NOT EXISTS (SELECT 1 FROM sale_items si WHERE si.sale_id = s.id AND si.product_id = p.id);

INSERT INTO sale_items (sale_id, product_id, quantity, list_unit_price_snapshot, discount_pct_snapshot, unit_price_snapshot, cost_price_snapshot)
SELECT s.id, p.id, 3, 18.99, 4.00, 18.23, 11.77
FROM sales s
JOIN products p ON p.sku = 'PNT-001'
WHERE s.notes = '[ANALYTICS_SEED] SALE-002'
  AND NOT EXISTS (SELECT 1 FROM sale_items si WHERE si.sale_id = s.id AND si.product_id = p.id);

-- Losers in analytics
INSERT INTO sale_items (sale_id, product_id, quantity, list_unit_price_snapshot, discount_pct_snapshot, unit_price_snapshot, cost_price_snapshot)
SELECT s.id, p.id, 5, 14.99, 10.00, 13.49, 18.90
FROM sales s
JOIN products p ON p.sku = 'MOCK-LOSS-001'
WHERE s.notes = '[ANALYTICS_SEED] SALE-003'
  AND NOT EXISTS (SELECT 1 FROM sale_items si WHERE si.sale_id = s.id AND si.product_id = p.id);

INSERT INTO sale_items (sale_id, product_id, quantity, list_unit_price_snapshot, discount_pct_snapshot, unit_price_snapshot, cost_price_snapshot)
SELECT s.id, p.id, 1, 89.99, 39.99, 54.00, 58.49
FROM sales s
JOIN products p ON p.sku = 'ELEC-006'
WHERE s.notes = '[ANALYTICS_SEED] SALE-003'
  AND NOT EXISTS (SELECT 1 FROM sale_items si WHERE si.sale_id = s.id AND si.product_id = p.id);

INSERT INTO sale_items (sale_id, product_id, quantity, list_unit_price_snapshot, discount_pct_snapshot, unit_price_snapshot, cost_price_snapshot)
SELECT s.id, p.id, 1, 34.99, 0.00, 34.99, 23.10
FROM sales s
JOIN products p ON p.sku = 'ELEC-001'
WHERE s.notes = '[ANALYTICS_SEED] SALE-003'
  AND NOT EXISTS (SELECT 1 FROM sale_items si WHERE si.sale_id = s.id AND si.product_id = p.id);

-- 4) Purchase orders
INSERT INTO purchase_orders (provider_id, status, desired_arrival_date, recommended_order_date, sent_at, received_at, notes, created_at, updated_at)
SELECT p.id, 'RECEIVED', CURDATE() - INTERVAL 8 DAY, CURDATE() - INTERVAL 12 DAY, NOW() - INTERVAL 13 DAY, NOW() - INTERVAL 9 DAY,
       '[ANALYTICS_SEED] PO-001', NOW() - INTERVAL 13 DAY, NOW() - INTERVAL 9 DAY
FROM providers p
WHERE p.name = 'Proveedor Mock Demo'
  AND NOT EXISTS (SELECT 1 FROM purchase_orders po WHERE po.notes = '[ANALYTICS_SEED] PO-001');

INSERT INTO purchase_orders (provider_id, status, desired_arrival_date, recommended_order_date, sent_at, received_at, notes, created_at, updated_at)
SELECT p.id, 'RECEIVED', CURDATE() - INTERVAL 3 DAY, CURDATE() - INTERVAL 6 DAY, NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 4 DAY,
       '[ANALYTICS_SEED] PO-002', NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 4 DAY
FROM providers p
WHERE p.name = 'Proveedor Mock Demo'
  AND NOT EXISTS (SELECT 1 FROM purchase_orders po WHERE po.notes = '[ANALYTICS_SEED] PO-002');

INSERT INTO purchase_order_items (order_id, product_id, quantity, unit_price_snapshot)
SELECT po.id, p.id, 24, 23.10
FROM purchase_orders po
JOIN products p ON p.sku = 'ELEC-001'
WHERE po.notes = '[ANALYTICS_SEED] PO-001'
  AND NOT EXISTS (SELECT 1 FROM purchase_order_items poi WHERE poi.order_id = po.id AND poi.product_id = p.id);

INSERT INTO purchase_order_items (order_id, product_id, quantity, unit_price_snapshot)
SELECT po.id, p.id, 120, 5.48
FROM purchase_orders po
JOIN products p ON p.sku = 'OFF-001'
WHERE po.notes = '[ANALYTICS_SEED] PO-001'
  AND NOT EXISTS (SELECT 1 FROM purchase_order_items poi WHERE poi.order_id = po.id AND poi.product_id = p.id);

INSERT INTO purchase_order_items (order_id, product_id, quantity, unit_price_snapshot)
SELECT po.id, p.id, 20, 18.90
FROM purchase_orders po
JOIN products p ON p.sku = 'MOCK-LOSS-001'
WHERE po.notes = '[ANALYTICS_SEED] PO-002'
  AND NOT EXISTS (SELECT 1 FROM purchase_order_items poi WHERE poi.order_id = po.id AND poi.product_id = p.id);

-- 5) Movements, alerts and price history (for app-wide modules)
INSERT INTO stock_movements (product_id, delta, reason, notes, moved_at)
SELECT p.id, -3, 'SALE', '[ANALYTICS_SEED] Movimiento venta demo', NOW() - INTERVAL 3 DAY
FROM products p
WHERE p.sku = 'ELEC-001'
  AND NOT EXISTS (
      SELECT 1 FROM stock_movements sm
      WHERE sm.product_id = p.id
        AND sm.notes = '[ANALYTICS_SEED] Movimiento venta demo'
  );

INSERT INTO stock_movements (product_id, delta, reason, notes, moved_at)
SELECT p.id, 15, 'PURCHASE', '[ANALYTICS_SEED] Movimiento compra demo', NOW() - INTERVAL 6 DAY
FROM products p
WHERE p.sku = 'OFF-001'
  AND NOT EXISTS (
      SELECT 1 FROM stock_movements sm
      WHERE sm.product_id = p.id
        AND sm.notes = '[ANALYTICS_SEED] Movimiento compra demo'
  );

INSERT INTO stock_alerts (product_id, alert_type, message, resolved, resolved_at, created_at)
SELECT p.id, 'LOW_STOCK', '[MOCK] Stock bajo detectado en Mechanical Keyboard.', 0, NULL, NOW() - INTERVAL 1 DAY
FROM products p
WHERE p.sku = 'ELEC-006'
  AND NOT EXISTS (
      SELECT 1 FROM stock_alerts sa
      WHERE sa.product_id = p.id
        AND sa.message = '[MOCK] Stock bajo detectado en Mechanical Keyboard.'
  );

INSERT INTO stock_alerts (product_id, alert_type, message, resolved, resolved_at, created_at)
SELECT p.id, 'PRICE_ANOMALY', '[MOCK] Cambio brusco de precio en Producto Demo Perdedor.', 0, NULL, NOW() - INTERVAL 2 DAY
FROM products p
WHERE p.sku = 'MOCK-LOSS-001'
  AND NOT EXISTS (
      SELECT 1 FROM stock_alerts sa
      WHERE sa.product_id = p.id
        AND sa.message = '[MOCK] Cambio brusco de precio en Producto Demo Perdedor.'
  );

INSERT INTO price_history (product_id, old_price, new_price, changed_by, changed_at)
SELECT p.id, 12.99, 14.99, 1, NOW() - INTERVAL 5 DAY
FROM products p
WHERE p.sku = 'MOCK-LOSS-001'
  AND NOT EXISTS (
      SELECT 1 FROM price_history ph
      WHERE ph.product_id = p.id
        AND ph.old_price = 12.99
        AND ph.new_price = 14.99
  );
