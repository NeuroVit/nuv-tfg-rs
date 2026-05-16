-- V23__seed_test_organizations.sql
-- Seed 3 test organizations with 2 years of analytics-ready data.
--   Org A: NovaTech Pro      — thriving electronics retailer  (~58% gross margin, growing)
--   Org B: MidMart Supply    — struggling office supplier     (~32% margin, flat)
--   Org C: ZeroEdge Trading  — borderline failing grocery     (~12% margin, declining)
-- All passwords: Admin1234!

-- ════════════════════════════════════════════════════════════════
-- 0. CLEAR DEFAULT ORG DATA  (FK-safe delete order)
-- ════════════════════════════════════════════════════════════════
DELETE FROM stock_alerts         WHERE organization_id = 1;
DELETE FROM stock_movements      WHERE organization_id = 1;
DELETE FROM sale_items           WHERE organization_id = 1;
DELETE FROM sales                WHERE organization_id = 1;
DELETE FROM purchase_order_items WHERE organization_id = 1;
DELETE FROM purchase_orders      WHERE organization_id = 1;
DELETE FROM price_history        WHERE organization_id = 1;
DELETE FROM bundle_items         WHERE organization_id = 1;
DELETE FROM bundles              WHERE organization_id = 1;
DELETE FROM clients              WHERE organization_id = 1;
DELETE FROM products             WHERE organization_id = 1;
DELETE FROM providers            WHERE organization_id = 1;
DELETE FROM categories           WHERE organization_id = 1;

-- ════════════════════════════════════════════════════════════════
-- 1. USERS  (password = Admin1234! for all six)
-- ════════════════════════════════════════════════════════════════
INSERT INTO users (nombre, apellidos, username, email, password_hash, role,
                   is_active, must_change_password, token_version, time_zone) VALUES
  ('Nova',  'Administrador',   'nova_admin',   'nova.admin@novatech.test',
   '$2a$12$8eVpSMpEBmIkjvuSFMNTCOGvT5b9TRrQ1BIFz7XtHlKmZ3v9DqPle',
   'ROLE_ADMIN',   1, 0, 0, 'Europe/Madrid'),
  ('Nova',  'Gerente', 'nova_manager', 'nova.manager@novatech.test',
   '$2a$12$8eVpSMpEBmIkjvuSFMNTCOGvT5b9TRrQ1BIFz7XtHlKmZ3v9DqPle',
   'ROLE_MANAGER', 1, 0, 0, 'Europe/Madrid'),
  ('Mid',   'Administrador',   'mid_admin',    'mid.admin@midmart.test',
   '$2a$12$8eVpSMpEBmIkjvuSFMNTCOGvT5b9TRrQ1BIFz7XtHlKmZ3v9DqPle',
   'ROLE_ADMIN',   1, 0, 0, 'Europe/Madrid'),
  ('Mid',   'Gerente', 'mid_manager',  'mid.manager@midmart.test',
   '$2a$12$8eVpSMpEBmIkjvuSFMNTCOGvT5b9TRrQ1BIFz7XtHlKmZ3v9DqPle',
   'ROLE_MANAGER', 1, 0, 0, 'Europe/Madrid'),
  ('Zero',  'Administrador',   'zero_admin',   'zero.admin@zeroedge.test',
   '$2a$12$8eVpSMpEBmIkjvuSFMNTCOGvT5b9TRrQ1BIFz7XtHlKmZ3v9DqPle',
   'ROLE_ADMIN',   1, 0, 0, 'Europe/Madrid'),
  ('Zero',  'Gerente', 'zero_manager', 'zero.manager@zeroedge.test',
   '$2a$12$8eVpSMpEBmIkjvuSFMNTCOGvT5b9TRrQ1BIFz7XtHlKmZ3v9DqPle',
   'ROLE_MANAGER', 1, 0, 0, 'Europe/Madrid');

-- multi-row INSERT: LAST_INSERT_ID() = ID of first inserted row
SET @nova_admin_id   = LAST_INSERT_ID();
SET @nova_manager_id = LAST_INSERT_ID() + 1;
SET @mid_admin_id    = LAST_INSERT_ID() + 2;
SET @mid_manager_id  = LAST_INSERT_ID() + 3;
SET @zero_admin_id   = LAST_INSERT_ID() + 4;
SET @zero_manager_id = LAST_INSERT_ID() + 5;

-- ════════════════════════════════════════════════════════════════
-- 2. ORGANIZATIONS
-- ════════════════════════════════════════════════════════════════
INSERT INTO organizations (name, slug) VALUES
  ('NovaTech Profesional',   'novatech-pro'),
  ('Suministros MidMart',    'midmart-supply'),
  ('Comercial ZeroEdge',     'zeroedge-trading');

SET @org_a_id = LAST_INSERT_ID();
SET @org_b_id = LAST_INSERT_ID() + 1;
SET @org_c_id = LAST_INSERT_ID() + 2;

-- ════════════════════════════════════════════════════════════════
-- 3. MEMBERSHIPS
-- ════════════════════════════════════════════════════════════════
INSERT INTO organization_memberships (organization_id, user_id, role) VALUES
  (@org_a_id, @nova_admin_id,   'OWNER'),
  (@org_a_id, @nova_manager_id, 'ADMIN'),
  (@org_b_id, @mid_admin_id,    'OWNER'),
  (@org_b_id, @mid_manager_id,  'ADMIN'),
  (@org_c_id, @zero_admin_id,   'OWNER'),
  (@org_c_id, @zero_manager_id, 'ADMIN');

-- ════════════════════════════════════════════════════════════════
-- 4. CATEGORIES (5 per org)
-- ════════════════════════════════════════════════════════════════
INSERT INTO categories (organization_id, name, description) VALUES
  (@org_a_id, 'Monitores',              'Pantallas para puestos de trabajo'),
  (@org_a_id, 'Teclados y Ratones',     'Periféricos de entrada y combos'),
  (@org_a_id, 'Auriculares y Audio',    'Auriculares, micrófonos y cámaras web'),
  (@org_a_id, 'Cables y Adaptadores',   'Cables de conectividad, concentradores y bases'),
  (@org_a_id, 'Accesorios',             'Soportes, brazos y extras ergonómicos'),
  (@org_b_id, 'Papel e Impresión',      'Papel, tóner y consumibles de impresión'),
  (@org_b_id, 'Bolígrafos y Escritura', 'Bolígrafos, rotuladores y correctores'),
  (@org_b_id, 'Archivo y Almacenaje',   'Archivadores, carpetas y sistemas de archivo'),
  (@org_b_id, 'Accesorios de Escritorio','Grapadoras, cinta y artículos de escritorio'),
  (@org_b_id, 'Presentación',           'Pizarras, caballetes y mandos de presentación'),
  (@org_c_id, 'Bebidas Calientes',      'Café, té y preparados de bebidas calientes'),
  (@org_c_id, 'Aperitivos y Dulces',    'Patatas, chocolate y aperitivos para llevar'),
  (@org_c_id, 'Cereales y Desayuno',    'Avena, cereales y básicos de desayuno'),
  (@org_c_id, 'Limpieza',               'Productos de limpieza para el hogar'),
  (@org_c_id, 'Bebidas',                'Bebidas frías, zumos y bebidas vegetales');

SET @cat_a1 = (SELECT id FROM categories WHERE organization_id=@org_a_id ORDER BY id LIMIT 1 OFFSET 0);
SET @cat_a2 = (SELECT id FROM categories WHERE organization_id=@org_a_id ORDER BY id LIMIT 1 OFFSET 1);
SET @cat_a3 = (SELECT id FROM categories WHERE organization_id=@org_a_id ORDER BY id LIMIT 1 OFFSET 2);
SET @cat_a4 = (SELECT id FROM categories WHERE organization_id=@org_a_id ORDER BY id LIMIT 1 OFFSET 3);
SET @cat_a5 = (SELECT id FROM categories WHERE organization_id=@org_a_id ORDER BY id LIMIT 1 OFFSET 4);
SET @cat_b1 = (SELECT id FROM categories WHERE organization_id=@org_b_id ORDER BY id LIMIT 1 OFFSET 0);
SET @cat_b2 = (SELECT id FROM categories WHERE organization_id=@org_b_id ORDER BY id LIMIT 1 OFFSET 1);
SET @cat_b3 = (SELECT id FROM categories WHERE organization_id=@org_b_id ORDER BY id LIMIT 1 OFFSET 2);
SET @cat_b4 = (SELECT id FROM categories WHERE organization_id=@org_b_id ORDER BY id LIMIT 1 OFFSET 3);
SET @cat_b5 = (SELECT id FROM categories WHERE organization_id=@org_b_id ORDER BY id LIMIT 1 OFFSET 4);
SET @cat_c1 = (SELECT id FROM categories WHERE organization_id=@org_c_id ORDER BY id LIMIT 1 OFFSET 0);
SET @cat_c2 = (SELECT id FROM categories WHERE organization_id=@org_c_id ORDER BY id LIMIT 1 OFFSET 1);
SET @cat_c3 = (SELECT id FROM categories WHERE organization_id=@org_c_id ORDER BY id LIMIT 1 OFFSET 2);
SET @cat_c4 = (SELECT id FROM categories WHERE organization_id=@org_c_id ORDER BY id LIMIT 1 OFFSET 3);
SET @cat_c5 = (SELECT id FROM categories WHERE organization_id=@org_c_id ORDER BY id LIMIT 1 OFFSET 4);

-- ════════════════════════════════════════════════════════════════
-- 5. PROVIDERS
-- ════════════════════════════════════════════════════════════════
INSERT INTO providers (organization_id, name, email, warehouse_location, lead_time_days, is_active) VALUES
  (@org_a_id, 'TechDistrib',             'pedidos@techdistrib.test', 'Centro Logístico Madrid',   4, 1),
  (@org_a_id, 'PerifPlus',               'inventario@periphplus.test',   'Almacén Barcelona',         5, 1),
  (@org_a_id, 'MundoCable',              'ventas@cableworld.test',   'Distribución Valencia',     3, 1),
  (@org_b_id, 'Suministros Oficina ES',  'pedidos@odepotes.test',   'Centro de Suministro Bilbao', 8, 1),
  (@org_b_id, 'RutaPapel',               'info@paperlane.test',     'Depósito Sevilla',         14, 1),
  (@org_c_id, 'Alimentos Rápidos',       'pedidos@quickfast.test',   'Mayorista Zaragoza',       15, 1);

SET @prov_a1 = (SELECT id FROM providers WHERE organization_id=@org_a_id ORDER BY id LIMIT 1 OFFSET 0);
SET @prov_a2 = (SELECT id FROM providers WHERE organization_id=@org_a_id ORDER BY id LIMIT 1 OFFSET 1);
SET @prov_a3 = (SELECT id FROM providers WHERE organization_id=@org_a_id ORDER BY id LIMIT 1 OFFSET 2);
SET @prov_b1 = (SELECT id FROM providers WHERE organization_id=@org_b_id ORDER BY id LIMIT 1 OFFSET 0);
SET @prov_b2 = (SELECT id FROM providers WHERE organization_id=@org_b_id ORDER BY id LIMIT 1 OFFSET 1);
SET @prov_c1 = (SELECT id FROM providers WHERE organization_id=@org_c_id ORDER BY id LIMIT 1 OFFSET 0);

-- ════════════════════════════════════════════════════════════════
-- 6. PRODUCTS  (25 per org, current_quantity=0 — updated at end)
--    Org A: ~58% margin  (acquisition = 42% of price)
--    Org B: ~32% margin  (acquisition = 68% of price)
--    Org C: ~12% margin  (acquisition = 88% of price)
-- ════════════════════════════════════════════════════════════════
INSERT INTO products (organization_id, name, sku, current_quantity, minimum_stock_level,
                      unit_price, acquisition_price, category_id, provider_id, version) VALUES
  -- ── Org A: Monitors (cat_a1, prov_a1) ─────────────────────
  (@org_a_id,'Monitor Ultrapanorámico 34"', 'NVT-MON-001', 0, 15, 499.99, 209.99, @cat_a1, @prov_a1, 0),
  (@org_a_id,'Pantalla 4K 27"',            'NVT-MON-002', 0, 20, 399.99, 167.99, @cat_a1, @prov_a1, 0),
  (@org_a_id,'Monitor Full HD 24"',        'NVT-MON-003', 0, 30, 199.99,  83.99, @cat_a1, @prov_a1, 0),
  (@org_a_id,'Monitor Portátil 15"',       'NVT-MON-004', 0, 15, 149.99,  62.99, @cat_a1, @prov_a1, 0),
  (@org_a_id,'Monitor Curvo 27"',          'NVT-MON-005', 0, 20, 299.99, 125.99, @cat_a1, @prov_a1, 0),
  -- ── Org A: Keyboards & Mice (cat_a2, prov_a2) ─────────────
  (@org_a_id,'Teclado Mecánico RGB',   'NVT-KBD-001', 0, 20, 129.99,  54.59, @cat_a2, @prov_a2, 0),
  (@org_a_id,'Teclado Inalámbrico',    'NVT-KBD-002', 0, 25, 69.99,  29.39, @cat_a2, @prov_a2, 0),
  (@org_a_id,'Ratón para Juegos 16000DPI','NVT-KBD-003', 0, 25, 79.99,  33.59, @cat_a2, @prov_a2, 0),
  (@org_a_id,'Ratón Inalámbrico',      'NVT-KBD-004', 0, 30, 49.99,  20.99, @cat_a2, @prov_a2, 0),
  (@org_a_id,'Combo Teclado y Ratón',  'NVT-KBD-005', 0, 20, 89.99,  37.79, @cat_a2, @prov_a2, 0),
  -- ── Org A: Headsets & Audio (cat_a3, prov_a2) ─────────────
  (@org_a_id,'Auriculares USB-C',      'NVT-AUD-001', 0, 15, 89.99,  37.79, @cat_a3, @prov_a2, 0),
  (@org_a_id,'Auriculares Bluetooth',  'NVT-AUD-002', 0, 20, 149.99, 62.99, @cat_a3, @prov_a2, 0),
  (@org_a_id,'Micrófono de Estudio',   'NVT-AUD-003', 0, 10, 119.99, 50.39, @cat_a3, @prov_a2, 0),
  (@org_a_id,'Cámara web 1080p',       'NVT-AUD-004', 0, 25, 79.99,  33.59, @cat_a3, @prov_a2, 0),
  (@org_a_id,'Auriculares con Cancelación de Ruido','NVT-AUD-005', 0, 15, 199.99, 83.99, @cat_a3, @prov_a2, 0),
  -- ── Org A: Cables & Adapters (cat_a4, prov_a3) ────────────
  (@org_a_id,'Concentrador USB-C 7 Puertos', 'NVT-CAB-001', 0, 30, 59.99,  25.19, @cat_a4, @prov_a3, 0),
  (@org_a_id,'Cable HDMI 2m',          'NVT-CAB-002', 0, 50, 14.99,   6.29, @cat_a4, @prov_a3, 0),
  (@org_a_id,'USB-C a DisplayPort',    'NVT-CAB-003', 0, 40, 24.99,  10.49, @cat_a4, @prov_a3, 0),
  (@org_a_id,'Base Thunderbolt 4',     'NVT-CAB-004', 0, 10, 149.99, 62.99, @cat_a4, @prov_a3, 0),
  (@org_a_id,'Batería Externa 20000mAh','NVT-CAB-005',0, 20, 49.99,  20.99, @cat_a4, @prov_a3, 0),
  -- ── Org A: Accessories (cat_a5, prov_a3) ──────────────────
  (@org_a_id,'Soporte de Portátil Aluminio','NVT-ACC-001', 0, 25, 39.99,  16.79, @cat_a5, @prov_a3, 0),
  (@org_a_id,'Brazo Doble para Monitor','NVT-ACC-002', 0, 12, 89.99,  37.79, @cat_a5, @prov_a3, 0),
  (@org_a_id,'Kit de Gestión de Cables','NVT-ACC-003', 0, 30, 19.99,   8.39, @cat_a5, @prov_a3, 0),
  (@org_a_id,'Filtro de Pantalla Antirreflejo','NVT-ACC-004',0, 20, 29.99,  12.59, @cat_a5, @prov_a3, 0),
  (@org_a_id,'Reposamuñecas Ergonómico','NVT-ACC-005', 0, 25, 24.99,  10.49, @cat_a5, @prov_a3, 0),
  -- ── Org B: Paper & Printing (cat_b1, prov_b1) ─────────────
  (@org_b_id,'Resma Papel A4 500h',        'MID-PPR-001', 0, 50, 5.99,  4.07, @cat_b1, @prov_b1, 0),
  (@org_b_id,'Papel Fotográfico Premium A4','MID-PPR-002', 0, 20, 14.99, 10.19, @cat_b1, @prov_b1, 0),
  (@org_b_id,'Tóner Láser Negro',          'MID-PPR-003', 0, 15, 39.99, 27.19, @cat_b1, @prov_b1, 0),
  (@org_b_id,'Paquete Múltiple Tinta Impresora',  'MID-PPR-004', 0, 20, 29.99, 20.39, @cat_b1, @prov_b1, 0),
  (@org_b_id,'Papel Carbón',               'MID-PPR-005', 0, 30, 8.99,  6.11, @cat_b1, @prov_b1, 0),
  -- ── Org B: Pens & Writing (cat_b2, prov_b1) ───────────────
  (@org_b_id,'Caja de Bolígrafos 50',    'MID-PEN-001', 0, 30, 7.99,  5.43, @cat_b2, @prov_b1, 0),
  (@org_b_id,'Set de Bolígrafo Líquido 10','MID-PEN-002',0, 25, 12.99, 8.83, @cat_b2, @prov_b1, 0),
  (@org_b_id,'Set de Subrayadores 8 Col','MID-PEN-003', 0, 30, 6.99,  4.75, @cat_b2, @prov_b1, 0),
  (@org_b_id,'Rotuladores Permanentes 12 ud','MID-PEN-004', 0, 25, 8.99,  6.11, @cat_b2, @prov_b1, 0),
  (@org_b_id,'Cinta Correctora 5 ud',    'MID-PEN-005', 0, 30, 9.99,  6.79, @cat_b2, @prov_b1, 0),
  -- ── Org B: Filing & Storage (cat_b3, prov_b2) ─────────────
  (@org_b_id,'Archivador de Anillas A4 50mm', 'MID-FIL-001', 0, 30, 5.99,  4.07, @cat_b3, @prov_b2, 0),
  (@org_b_id,'Caja Carpetas Colgantes 50',    'MID-FIL-002', 0, 15, 22.99, 15.63, @cat_b3, @prov_b2, 0),
  (@org_b_id,'Carpetas Plásticas 50 ud',      'MID-FIL-003', 0, 20, 14.99, 10.19, @cat_b3, @prov_b2, 0),
  (@org_b_id,'Archivador 3 Cajones',          'MID-FIL-004', 0, 5,  59.99, 40.79, @cat_b3, @prov_b2, 0),
  (@org_b_id,'Fundas para Documentos 100 ud', 'MID-FIL-005', 0, 20, 11.99, 8.15, @cat_b3, @prov_b2, 0),
  -- ── Org B: Desk Accessories (cat_b4, prov_b1) ─────────────
  (@org_b_id,'Grapadora 30 Hojas',     'MID-DSK-001', 0, 20, 12.99, 8.83, @cat_b4, @prov_b1, 0),
  (@org_b_id,'Grapas 5000 ud',         'MID-DSK-002', 0, 40, 3.99,  2.71, @cat_b4, @prov_b1, 0),
  (@org_b_id,'Dispensador de Cinta + Rollos', 'MID-DSK-003', 0, 25, 7.99,  5.43, @cat_b4, @prov_b1, 0),
  (@org_b_id,'Tijeras Oficina Profesional',    'MID-DSK-004', 0, 30, 6.99,  4.75, @cat_b4, @prov_b1, 0),
  (@org_b_id,'Notas Adhesivas Paquete 6',      'MID-DSK-005', 0, 35, 8.99,  6.11, @cat_b4, @prov_b1, 0),
  -- ── Org B: Presentation (cat_b5, prov_b2) ─────────────────
  (@org_b_id,'Pizarra Blanca 90x60cm', 'MID-PRE-001', 0, 8,  34.99, 23.79, @cat_b5, @prov_b2, 0),
  (@org_b_id,'Rotuladores Pizarra 8 ud','MID-PRE-002', 0, 30, 7.99,  5.43, @cat_b5, @prov_b2, 0),
  (@org_b_id,'Caballete para Bloc',    'MID-PRE-003', 0, 5,  49.99, 33.99, @cat_b5, @prov_b2, 0),
  (@org_b_id,'Mando para Presentaciones','MID-PRE-004',0, 15, 24.99, 16.99, @cat_b5, @prov_b2, 0),
  (@org_b_id,'Blocs para Caballete 2 ud','MID-PRE-005',0, 20, 16.99, 11.55, @cat_b5, @prov_b2, 0),
  -- ── Org C: Hot Drinks (cat_c1, prov_c1) ───────────────────
  (@org_c_id,'Café Molido 500g',       'ZRO-HOT-001', 0, 20, 7.99,  7.03, @cat_c1, @prov_c1, 0),
  (@org_c_id,'Café Soluble 200g',      'ZRO-HOT-002', 0, 25, 4.99,  4.39, @cat_c1, @prov_c1, 0),
  (@org_c_id,'Bolsitas de Té 80 ud',   'ZRO-HOT-003', 0, 30, 3.49,  3.07, @cat_c1, @prov_c1, 0),
  (@org_c_id,'Preparado Chocolate Caliente 500g', 'ZRO-HOT-004', 0, 20, 5.99,  5.27, @cat_c1, @prov_c1, 0),
  (@org_c_id,'Infusiones Surtidas 40 ud','ZRO-HOT-005',0, 25, 2.99,  2.63, @cat_c1, @prov_c1, 0),
  -- ── Org C: Snacks & Sweets (cat_c2, prov_c1) ─────────────
  (@org_c_id,'Patatas Fritas 150g',    'ZRO-SNK-001', 0, 40, 1.79,  1.58, @cat_c2, @prov_c1, 0),
  (@org_c_id,'Frutos Secos Mixtos 200g','ZRO-SNK-002', 0, 30, 3.99,  3.51, @cat_c2, @prov_c1, 0),
  (@org_c_id,'Tableta de Chocolate 100g','ZRO-SNK-003', 0, 50, 1.49,  1.31, @cat_c2, @prov_c1, 0),
  (@org_c_id,'Tortitas de Arroz 130g', 'ZRO-SNK-004', 0, 35, 2.29,  2.02, @cat_c2, @prov_c1, 0),
  (@org_c_id,'Barritas de Granola 6 ud','ZRO-SNK-005', 0, 30, 3.49,  3.07, @cat_c2, @prov_c1, 0),
  -- ── Org C: Cereals & Breakfast (cat_c3, prov_c1) ──────────
  (@org_c_id,'Copos de Avena 1kg',     'ZRO-CRL-001', 0, 30, 2.49,  2.19, @cat_c3, @prov_c1, 0),
  (@org_c_id,'Copos de Maíz 750g',     'ZRO-CRL-002', 0, 25, 3.29,  2.90, @cat_c3, @prov_c1, 0),
  (@org_c_id,'Muesli 500g',            'ZRO-CRL-003', 0, 20, 3.99,  3.51, @cat_c3, @prov_c1, 0),
  (@org_c_id,'Preparado para Tortitas 500g','ZRO-CRL-004', 0, 20, 2.99,  2.63, @cat_c3, @prov_c1, 0),
  (@org_c_id,'Granola Proteica 400g',  'ZRO-CRL-005', 0, 15, 6.99,  6.15, @cat_c3, @prov_c1, 0),
  -- ── Org C: Cleaning (cat_c4, prov_c1) ─────────────────────
  (@org_c_id,'Spray Multiusos 500ml',  'ZRO-CLN-001',0,30, 2.49,  2.19, @cat_c4, @prov_c1, 0),
  (@org_c_id,'Lavavajillas Líquido 500ml','ZRO-CLN-002', 0, 40, 1.99,  1.75, @cat_c4, @prov_c1, 0),
  (@org_c_id,'Limpiador de Suelos 1L', 'ZRO-CLN-003', 0, 25, 3.49,  3.07, @cat_c4, @prov_c1, 0),
  (@org_c_id,'Bolsas de Basura 30 ud', 'ZRO-CLN-004', 0, 20, 4.99,  4.39, @cat_c4, @prov_c1, 0),
  (@org_c_id,'Esponjas 10 ud',         'ZRO-CLN-005', 0, 30, 2.99,  2.63, @cat_c4, @prov_c1, 0),
  -- ── Org C: Beverages (cat_c5, prov_c1) ────────────────────
  (@org_c_id,'Agua con Gas 1.5L',      'ZRO-BEV-001', 0, 50, 1.09,  0.96, @cat_c5, @prov_c1, 0),
  (@org_c_id,'Zumo de Naranja 1L',     'ZRO-BEV-002', 0, 30, 2.49,  2.19, @cat_c5, @prov_c1, 0),
  (@org_c_id,'Bebida de Avena 1L',     'ZRO-BEV-003', 0, 25, 2.19,  1.93, @cat_c5, @prov_c1, 0),
  (@org_c_id,'Bebida Isotónica 500ml', 'ZRO-BEV-004', 0, 35, 1.89,  1.66, @cat_c5, @prov_c1, 0),
  (@org_c_id,'Batido 330ml',           'ZRO-BEV-005', 0, 25, 2.99,  2.63, @cat_c5, @prov_c1, 0);

-- ════════════════════════════════════════════════════════════════
-- 7. PRICE HISTORY  (spread over 2 years)
--    Org A: 6 price increases per product  (healthy pricing power)
--    Org B: 4 changes per product  (mix of cuts & increases)
--    Org C: 4 price cuts per product  (desperate reductions)
-- ════════════════════════════════════════════════════════════════

-- Org A: 6 increases, ~120 days apart, each +5% step back in time
INSERT INTO price_history (organization_id, product_id, old_price, new_price, changed_by, changed_at)
WITH steps AS (
  SELECT 1 s UNION ALL SELECT 2 UNION ALL SELECT 3
  UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
)
SELECT p.organization_id,
       p.id,
       ROUND(p.unit_price / POW(1.05, 7 - s.s), 2),   -- old (lower in the past)
       ROUND(p.unit_price / POW(1.05, 6 - s.s), 2),   -- new
       @nova_admin_id,
       DATE_SUB(NOW(), INTERVAL (720 - s.s * 120) DAY)
FROM products p, steps s
WHERE p.organization_id = @org_a_id;

-- Org B: 4 mixed changes (increase then cut then increase then cut)
INSERT INTO price_history (organization_id, product_id, old_price, new_price, changed_by, changed_at)
WITH steps AS (SELECT 1 s UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4)
SELECT p.organization_id,
       p.id,
       ROUND(p.unit_price * CASE s.s
         WHEN 1 THEN 0.92 WHEN 2 THEN 0.97 WHEN 3 THEN 0.90 ELSE 0.95 END, 2),
       ROUND(p.unit_price * CASE s.s
         WHEN 1 THEN 0.97 WHEN 2 THEN 0.90 WHEN 3 THEN 0.95 ELSE 1.00 END, 2),
       @mid_admin_id,
       DATE_SUB(NOW(), INTERVAL (700 - s.s * 160) DAY)
FROM products p, steps s
WHERE p.organization_id = @org_b_id;

-- Org C: 4 price cuts, each -5% step, prices were higher in the past
INSERT INTO price_history (organization_id, product_id, old_price, new_price, changed_by, changed_at)
WITH steps AS (SELECT 1 s UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4)
SELECT p.organization_id,
       p.id,
       ROUND(p.unit_price * POW(1.05, 5 - s.s), 2),   -- old (higher in the past)
       ROUND(p.unit_price * POW(1.05, 4 - s.s), 2),   -- new
       @zero_admin_id,
       DATE_SUB(NOW(), INTERVAL (680 - s.s * 160) DAY)
FROM products p, steps s
WHERE p.organization_id = @org_c_id;

-- ════════════════════════════════════════════════════════════════
-- 8. CLIENTS
-- ════════════════════════════════════════════════════════════════
INSERT INTO clients (organization_id, name, email, phone, is_active) VALUES
  (@org_a_id,'Acme Corporación',      'compras@acme.test',        '+34 91 100 0001', 1),
  (@org_a_id,'Agencia CieloAzul',     'administracion@bluesky.test', '+34 91 100 0002', 1),
  (@org_a_id,'Estudios Vertex',       'oficina@vertex.test',      '+34 91 100 0003', 1),
  (@org_a_id,'Nexum Informática',     'inventario@nexum.test',    '+34 91 100 0004', 1),
  (@org_a_id,'Fortis Legal',          'administracion@fortis.test','+34 91 100 0005',1),
  (@org_a_id,'Medios Órbita',         'compras@orbit.test',       '+34 93 200 0006', 1),
  (@org_a_id,'Analítica Crest',       'pedidos@crest.test',       '+34 93 200 0007', 1),
  (@org_a_id,'Diseño Cúspide',        'suministros@pinnacle.test','+34 93 200 0008', 1),
  (@org_a_id,'Consultoría Apex',      'compras@apex.test',        '+34 96 300 0009', 1),
  (@org_a_id,'Soluciones ZenSoft',    'informatica@zensoft.test', '+34 96 300 0010', 1),
  (@org_a_id,'Emprendimientos Maple', 'oficina@maple.test',       '+34 94 400 0011', 1),
  (@org_a_id,'Seguridad Ironclad',    'pedidos@ironclad.test',    '+34 94 400 0012', 1),

  (@org_b_id,'Administración RutaClara', 'pedidos@clearpath.test',   '+34 91 500 0001', 1),
  (@org_b_id,'Oficina Redwood',          'administracion@redwood.test', '+34 91 500 0002', 1),
  (@org_b_id,'Notaría Goldstar',         'suministros@goldstar.test','+34 91 500 0003', 1),
  (@org_b_id,'Socios Birch',             'compras@birch.test',       '+34 93 600 0004', 1),
  (@org_b_id,'Asoc. Cedarwood',          'oficina@cedarwood.test',   '+34 93 600 0005', 1),
  (@org_b_id,'Bufete Silverthorn',       'administracion@silverthorn.test', '+34 93 600 0006', 1),
  (@org_b_id,'RRHH NorthPeak',           'pedidos@northpeak.test',   '+34 94 700 0007', 1),
  (@org_b_id,'Impresión Crossroads',     'pedidos@crossroads.test',  '+34 94 700 0008', 1),
  (@org_b_id,'Servicios BlueLine',       'compras@blueline.test',    '+34 94 700 0009', 1),
  (@org_b_id,'Inmuebles Harbour',        'oficina@harbour.test',     '+34 96 800 0010', 1),

  (@org_c_id,'Casa Martínez',      'pedidos@casamartinez.test','+34 91 900 0001', 1),
  (@org_c_id,'Cafeteria Sol',      'compras@cafeteriasol.test','+34 91 900 0002', 1),
  (@org_c_id,'Bar La Esquina',     'pedidos@laesquina.test',   '+34 91 900 0003', 1),
  (@org_c_id,'Tienda El Rincón',   'inventario@elrincon.test', '+34 91 900 0004', 1),
  (@org_c_id,'Minimercado Lola',   'pedidos@mmlola.test',      '+34 91 900 0005', 1),
  (@org_c_id,'Bodega Norte',       'compras@bodeganorte.test', '+34 91 900 0006', 1),
  (@org_c_id,'Kiosko Central',     'pedidos@kioskoc.test',     '+34 91 900 0007', 1),
  (@org_c_id,'Despacho Rápido',    'administracion@despachorapido.test','+34 91 900 0008', 1);

-- ════════════════════════════════════════════════════════════════
-- 9. BUNDLES + BUNDLE ITEMS
-- ════════════════════════════════════════════════════════════════
INSERT INTO bundles (organization_id, name, description, discount_pct, active) VALUES
  (@org_a_id,'Kit de Oficina',      'Monitor + teclado + cámara web', 12.00, 1),
  (@org_a_id,'Lote para Juegos',    'Ratón para juegos + teclado RGB + auriculares', 15.00, 1),
  (@org_a_id,'Kit Creador Estudio', 'Cámara web + micrófono + concentrador USB', 10.00, 1),
  (@org_a_id,'Lote de Viaje',       'Monitor portátil + soporte portátil + concentrador USB-C', 10.00, 1),
  (@org_b_id,'Lote Escritorio',     'Papel + bolígrafos + grapadora', 8.00, 1),
  (@org_b_id,'Kit Archivo',         'Archivadores + carpetas + carpetas colgantes', 7.00, 1),
  (@org_c_id,'Desayuno Económico',  'Avena + té + café', 5.00, 1);

SET @bun_a1 = (SELECT id FROM bundles WHERE organization_id=@org_a_id ORDER BY id LIMIT 1 OFFSET 0);
SET @bun_a2 = (SELECT id FROM bundles WHERE organization_id=@org_a_id ORDER BY id LIMIT 1 OFFSET 1);
SET @bun_a3 = (SELECT id FROM bundles WHERE organization_id=@org_a_id ORDER BY id LIMIT 1 OFFSET 2);
SET @bun_a4 = (SELECT id FROM bundles WHERE organization_id=@org_a_id ORDER BY id LIMIT 1 OFFSET 3);
SET @bun_b1 = (SELECT id FROM bundles WHERE organization_id=@org_b_id ORDER BY id LIMIT 1 OFFSET 0);
SET @bun_b2 = (SELECT id FROM bundles WHERE organization_id=@org_b_id ORDER BY id LIMIT 1 OFFSET 1);
SET @bun_c1 = (SELECT id FROM bundles WHERE organization_id=@org_c_id ORDER BY id LIMIT 1 OFFSET 0);

-- Bundle items — reference product names to get safe IDs
INSERT INTO bundle_items (bundle_id, product_id, organization_id, quantity)
SELECT @bun_a1, id, @org_a_id, 1 FROM products WHERE organization_id=@org_a_id AND sku IN ('NVT-MON-003','NVT-KBD-002','NVT-AUD-004')
UNION ALL
SELECT @bun_a2, id, @org_a_id, 1 FROM products WHERE organization_id=@org_a_id AND sku IN ('NVT-KBD-003','NVT-KBD-001','NVT-AUD-001')
UNION ALL
SELECT @bun_a3, id, @org_a_id, 1 FROM products WHERE organization_id=@org_a_id AND sku IN ('NVT-AUD-004','NVT-AUD-003','NVT-CAB-001')
UNION ALL
SELECT @bun_a4, id, @org_a_id, 1 FROM products WHERE organization_id=@org_a_id AND sku IN ('NVT-MON-004','NVT-ACC-001','NVT-CAB-001')
UNION ALL
SELECT @bun_b1, id, @org_b_id, 1 FROM products WHERE organization_id=@org_b_id AND sku IN ('MID-PPR-001','MID-PEN-001','MID-DSK-001')
UNION ALL
SELECT @bun_b2, id, @org_b_id, 1 FROM products WHERE organization_id=@org_b_id AND sku IN ('MID-FIL-001','MID-FIL-003','MID-FIL-002')
UNION ALL
SELECT @bun_c1, id, @org_c_id, 1 FROM products WHERE organization_id=@org_c_id AND sku IN ('ZRO-CRL-001','ZRO-HOT-003','ZRO-HOT-001');

-- ════════════════════════════════════════════════════════════════
-- 10. PURCHASE ORDERS  (monthly over 24 months)
-- ════════════════════════════════════════════════════════════════
-- Helper CTE for months 1..24 (used inline in each INSERT)

-- Org A: 24 monthly orders, rotated among 3 providers, always on time
INSERT INTO purchase_orders (organization_id, provider_id, status,
  desired_arrival_date, recommended_order_date, sent_at, received_at, created_at)
WITH mo AS (
  SELECT 1 m UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
  UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12
  UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL SELECT 16
  UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
  UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24
)
SELECT @org_a_id,
  CASE MOD(m-1,3) WHEN 0 THEN @prov_a1 WHEN 1 THEN @prov_a2 ELSE @prov_a3 END,
  'RECEIVED',
  DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL (25-m) MONTH), '%Y-%m-15'),
  DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL (25-m) MONTH), '%Y-%m-09'),
  CONCAT(DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL (25-m) MONTH), '%Y-%m-10'), ' 09:00:00'),
  CONCAT(DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL (25-m) MONTH), '%Y-%m-14'), ' 15:00:00'),
  CONCAT(DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL (25-m) MONTH), '%Y-%m-05'), ' 09:00:00')
FROM mo;

-- Org B: 24 monthly orders, 2 providers, ~25% late
INSERT INTO purchase_orders (organization_id, provider_id, status,
  desired_arrival_date, recommended_order_date, sent_at, received_at, created_at)
WITH mo AS (
  SELECT 1 m UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
  UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12
  UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL SELECT 16
  UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
  UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24
)
SELECT @org_b_id,
  CASE MOD(m-1,2) WHEN 0 THEN @prov_b1 ELSE @prov_b2 END,
  'RECEIVED',
  DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL (25-m) MONTH), '%Y-%m-15'),
  DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL (25-m) MONTH), '%Y-%m-05'),
  CONCAT(DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL (25-m) MONTH), '%Y-%m-07'), ' 10:00:00'),
  -- Every 4th order is 5 days late
  CONCAT(DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL (25-m) MONTH),
    CASE WHEN MOD(m,4)=0 THEN '%Y-%m-20' ELSE '%Y-%m-15' END), ' 14:00:00'),
  CONCAT(DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL (25-m) MONTH), '%Y-%m-02'), ' 09:00:00')
FROM mo;

-- Org C: 12 bimonthly orders (every 2 months), single provider, ~40% late
INSERT INTO purchase_orders (organization_id, provider_id, status,
  desired_arrival_date, recommended_order_date, sent_at, received_at, created_at)
WITH mo AS (
  SELECT 1 m UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
  UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12
)
SELECT @org_c_id, @prov_c1, 'RECEIVED',
  DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL (13-m)*2 MONTH), '%Y-%m-15'),
  DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL (13-m)*2 MONTH), '%Y-%m-01'),
  CONCAT(DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL (13-m)*2 MONTH), '%Y-%m-02'), ' 09:00:00'),
  -- 40% late (every 5th out of... actually every m where MOD(m,5)>2 is late)
  CONCAT(DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL (13-m)*2 MONTH),
    CASE WHEN MOD(m,5) > 2 THEN '%Y-%m-22' ELSE '%Y-%m-14' END), ' 16:00:00'),
  CONCAT(DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL (13-m)*2 MONTH), '%Y-%m-01'), ' 08:00:00')
FROM mo;

-- ════════════════════════════════════════════════════════════════
-- 11. PURCHASE ORDER ITEMS
--     Each order gets ~5 products (MOD filter gives 5 out of 25)
-- ════════════════════════════════════════════════════════════════
INSERT INTO purchase_order_items (order_id, organization_id, product_id, quantity, unit_price_snapshot)
SELECT po.id, @org_a_id, p.id,
  40 + MOD(po.id + p.id, 60),    -- quantity 40-99
  p.acquisition_price
FROM purchase_orders po
JOIN products p ON p.organization_id = @org_a_id
WHERE po.organization_id = @org_a_id
  AND MOD(po.id + p.id, 5) = 0;

INSERT INTO purchase_order_items (order_id, organization_id, product_id, quantity, unit_price_snapshot)
SELECT po.id, @org_b_id, p.id,
  30 + MOD(po.id + p.id, 50),
  p.acquisition_price
FROM purchase_orders po
JOIN products p ON p.organization_id = @org_b_id
WHERE po.organization_id = @org_b_id
  AND MOD(po.id + p.id, 5) = 0;

INSERT INTO purchase_order_items (order_id, organization_id, product_id, quantity, unit_price_snapshot)
SELECT po.id, @org_c_id, p.id,
  20 + MOD(po.id + p.id, 30),
  p.acquisition_price
FROM purchase_orders po
JOIN products p ON p.organization_id = @org_c_id
WHERE po.organization_id = @org_c_id
  AND MOD(po.id + p.id, 5) = 0;

-- ════════════════════════════════════════════════════════════════
-- 12. STOCK MOVEMENTS — INITIAL (one per product)
-- ════════════════════════════════════════════════════════════════
INSERT INTO stock_movements (organization_id, product_id, delta, reason, notes, moved_at)
SELECT organization_id, id,
  CASE
    WHEN organization_id = @org_a_id THEN 100 + MOD(id, 100)   -- 100-199 units
    WHEN organization_id = @org_b_id THEN  60 + MOD(id,  60)   --  60-119 units
    ELSE                                   20 + MOD(id,  25)   --  20-44 units
  END,
  'INITIAL', 'Stock inicial', DATE_SUB(NOW(), INTERVAL 730 DAY)
FROM products
WHERE organization_id IN (@org_a_id, @org_b_id, @org_c_id);

-- ════════════════════════════════════════════════════════════════
-- 13. STOCK MOVEMENTS — PURCHASE (from received order items)
-- ════════════════════════════════════════════════════════════════
INSERT INTO stock_movements (organization_id, product_id, delta, reason, notes, moved_at)
SELECT poi.organization_id, poi.product_id, poi.quantity, 'PURCHASE',
  CONCAT('Pedido #', poi.order_id),
  po.received_at
FROM purchase_order_items poi
JOIN purchase_orders po ON po.id = poi.order_id
WHERE poi.organization_id IN (@org_a_id, @org_b_id, @org_c_id);

-- ════════════════════════════════════════════════════════════════
-- 14. SALES  (2-year history per org)
-- Numbers helper: cross-join of n0×n0×n0 gives 0..999; filter to 1..729
-- ════════════════════════════════════════════════════════════════

-- ── Org A: thriving, growing ──────────────────────────────────
-- Year 1 (days 365-729): 2 sales/weekday, 1/weekend
-- Year 2 (days 1-364):   3 sales/weekday, 1/weekend  → ~50% more volume
INSERT INTO sales (organization_id, client_id, sold_at,
  subtotal_amount, discount_total_amount, global_discount_pct, total_amount,
  created_by, created_at)
WITH n0 AS (
  SELECT 0 v UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
),
nums AS (
  SELECT a.v*100+b.v*10+c.v AS n
  FROM n0 a, n0 b, n0 c
  WHERE a.v*100+b.v*10+c.v BETWEEN 1 AND 729
),
dates AS (
  SELECT n, DATE_SUB(CURDATE(), INTERVAL n DAY) AS d,
         DAYOFWEEK(DATE_SUB(CURDATE(), INTERVAL n DAY)) AS dow
  FROM nums
),
slots AS (SELECT 0 s UNION ALL SELECT 1 UNION ALL SELECT 2),
client_list AS (
  SELECT id, ROW_NUMBER() OVER (ORDER BY id) - 1 AS rn
  FROM clients WHERE organization_id = @org_a_id
),
sale_rows AS (
  SELECT d.n, d.d, sl.s,
    (SELECT id FROM client_list WHERE rn = MOD(d.n*3 + sl.s, 12)) AS cid
  FROM dates d JOIN slots sl ON 1=1
  WHERE (d.dow BETWEEN 2 AND 6 AND ((d.n > 364 AND sl.s < 2) OR (d.n <= 364 AND sl.s < 3)))
     OR (d.dow NOT IN (2,3,4,5,6) AND sl.s = 0)
)
SELECT @org_a_id, cid,
  TIMESTAMP(d, SEC_TO_TIME(28800 + MOD(n*1237 + s*541, 32400))),
  0.00, 0.00, 0.00, 0.00,
  @nova_admin_id, NOW()
FROM sale_rows;

-- ── Org B: struggling, flat ───────────────────────────────────
-- 1 sale on most weekdays (skip every 5th), no weekends
INSERT INTO sales (organization_id, client_id, sold_at,
  subtotal_amount, discount_total_amount, global_discount_pct, total_amount,
  created_by, created_at)
WITH n0 AS (
  SELECT 0 v UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
),
nums AS (SELECT a.v*100+b.v*10+c.v AS n FROM n0 a, n0 b, n0 c
         WHERE a.v*100+b.v*10+c.v BETWEEN 1 AND 729),
dates AS (
  SELECT n, DATE_SUB(CURDATE(), INTERVAL n DAY) AS d,
         DAYOFWEEK(DATE_SUB(CURDATE(), INTERVAL n DAY)) AS dow
  FROM nums
),
client_list AS (
  SELECT id, ROW_NUMBER() OVER (ORDER BY id) - 1 AS rn
  FROM clients WHERE organization_id = @org_b_id
)
SELECT @org_b_id,
  (SELECT id FROM client_list WHERE rn = MOD(n*7, 10)),
  TIMESTAMP(d, SEC_TO_TIME(28800 + MOD(n*1031, 28800))),
  0.00, 0.00, 0.00, 0.00,
  @mid_admin_id, NOW()
FROM dates
WHERE dow BETWEEN 2 AND 6
  AND MOD(n, 5) <> 0;  -- skip ~20% of weekdays

-- ── Org C: failing, declining ────────────────────────────────
-- Year 1 (days 366-729): sale every 2 days
-- Year 2 (days 1-365):   sale every 3 days  → volume drops ~33%
INSERT INTO sales (organization_id, client_id, sold_at,
  subtotal_amount, discount_total_amount, global_discount_pct, total_amount,
  created_by, created_at)
WITH n0 AS (
  SELECT 0 v UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
),
nums AS (SELECT a.v*100+b.v*10+c.v AS n FROM n0 a, n0 b, n0 c
         WHERE a.v*100+b.v*10+c.v BETWEEN 1 AND 729),
client_list AS (
  SELECT id, ROW_NUMBER() OVER (ORDER BY id) - 1 AS rn
  FROM clients WHERE organization_id = @org_c_id
)
SELECT @org_c_id,
  (SELECT id FROM client_list WHERE rn = MOD(n*11, 8)),
  TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL n DAY), SEC_TO_TIME(28800 + MOD(n*887, 25200))),
  0.00, 0.00, 0.00, 0.00,
  @zero_admin_id, NOW()
FROM nums
WHERE (n > 365 AND MOD(n,2) = 0)
   OR (n <= 365 AND MOD(n,3) = 0);

-- ════════════════════════════════════════════════════════════════
-- 15. SALE ITEMS — primary item (1 product per sale, rotating)
-- Every 10th sale is reserved for bundle attribution (handled next)
-- ════════════════════════════════════════════════════════════════

-- Org A primary items
INSERT INTO sale_items (sale_id, organization_id, product_id, bundle_id,
  quantity, list_unit_price_snapshot, discount_pct_snapshot, unit_price_snapshot, cost_price_snapshot)
WITH product_rank AS (
  SELECT id, unit_price, acquisition_price,
    ROW_NUMBER() OVER (ORDER BY id) - 1 AS rn
  FROM products WHERE organization_id = @org_a_id
)
SELECT s.id, @org_a_id, pr.id, NULL,
  1 + MOD(s.id, 3),
  pr.unit_price, 0.00, pr.unit_price, pr.acquisition_price
FROM sales s
JOIN product_rank pr ON pr.rn = MOD(s.id, 25)
WHERE s.organization_id = @org_a_id AND MOD(s.id, 10) <> 0;

-- Org A second item on every 3rd non-bundle sale (for co-occurrence data)
INSERT INTO sale_items (sale_id, organization_id, product_id, bundle_id,
  quantity, list_unit_price_snapshot, discount_pct_snapshot, unit_price_snapshot, cost_price_snapshot)
WITH product_rank AS (
  SELECT id, unit_price, acquisition_price,
    ROW_NUMBER() OVER (ORDER BY id) - 1 AS rn
  FROM products WHERE organization_id = @org_a_id
)
SELECT s.id, @org_a_id, pr.id, NULL,
  1,
  pr.unit_price, 0.00, pr.unit_price, pr.acquisition_price
FROM sales s
JOIN product_rank pr ON pr.rn = MOD(s.id + 7, 25)
WHERE s.organization_id = @org_a_id
  AND MOD(s.id, 10) <> 0
  AND MOD(s.id, 3) = 0
  AND MOD(s.id, 25) <> MOD(s.id + 7, 25);

-- Org A bundle sales (every 10th sale → bundle attribution)
INSERT INTO sale_items (sale_id, organization_id, product_id, bundle_id,
  quantity, list_unit_price_snapshot, discount_pct_snapshot, unit_price_snapshot, cost_price_snapshot)
WITH bundle_rank AS (
  SELECT id AS bid, discount_pct, ROW_NUMBER() OVER (ORDER BY id) - 1 AS brn
  FROM bundles WHERE organization_id = @org_a_id
)
SELECT s.id, @org_a_id, bi.product_id, br.bid,
  1,
  p.unit_price,
  br.discount_pct,
  ROUND(p.unit_price * (1 - br.discount_pct/100), 2),
  p.acquisition_price
FROM sales s
JOIN bundle_rank br ON br.brn = MOD(FLOOR(s.id / 10), 4)
JOIN bundle_items bi ON bi.bundle_id = br.bid AND bi.organization_id = @org_a_id
JOIN products p ON p.id = bi.product_id
WHERE s.organization_id = @org_a_id AND MOD(s.id, 10) = 0;

-- Org B primary items
INSERT INTO sale_items (sale_id, organization_id, product_id, bundle_id,
  quantity, list_unit_price_snapshot, discount_pct_snapshot, unit_price_snapshot, cost_price_snapshot)
WITH product_rank AS (
  SELECT id, unit_price, acquisition_price,
    ROW_NUMBER() OVER (ORDER BY id) - 1 AS rn
  FROM products WHERE organization_id = @org_b_id
)
SELECT s.id, @org_b_id, pr.id, NULL,
  2 + MOD(s.id * 5, 4),   -- qty 2-5 (office bulk orders)
  pr.unit_price, 0.00, pr.unit_price, pr.acquisition_price
FROM sales s
JOIN product_rank pr ON pr.rn = MOD(s.id, 25)
WHERE s.organization_id = @org_b_id AND MOD(s.id, 8) <> 0;

-- Org B bundle sales (every 8th sale)
INSERT INTO sale_items (sale_id, organization_id, product_id, bundle_id,
  quantity, list_unit_price_snapshot, discount_pct_snapshot, unit_price_snapshot, cost_price_snapshot)
WITH bundle_rank AS (
  SELECT id AS bid, discount_pct, ROW_NUMBER() OVER (ORDER BY id) - 1 AS brn
  FROM bundles WHERE organization_id = @org_b_id
)
SELECT s.id, @org_b_id, bi.product_id, br.bid,
  2,
  p.unit_price,
  br.discount_pct,
  ROUND(p.unit_price * (1 - br.discount_pct/100), 2),
  p.acquisition_price
FROM sales s
JOIN bundle_rank br ON br.brn = MOD(FLOOR(s.id / 8), 2)
JOIN bundle_items bi ON bi.bundle_id = br.bid AND bi.organization_id = @org_b_id
JOIN products p ON p.id = bi.product_id
WHERE s.organization_id = @org_b_id AND MOD(s.id, 8) = 0;

-- Org C primary items (tiny quantities)
INSERT INTO sale_items (sale_id, organization_id, product_id, bundle_id,
  quantity, list_unit_price_snapshot, discount_pct_snapshot, unit_price_snapshot, cost_price_snapshot)
WITH product_rank AS (
  SELECT id, unit_price, acquisition_price,
    ROW_NUMBER() OVER (ORDER BY id) - 1 AS rn
  FROM products WHERE organization_id = @org_c_id
)
SELECT s.id, @org_c_id, pr.id, NULL,
  1 + MOD(s.id * 7, 2),   -- qty 1-2
  pr.unit_price, 0.00, pr.unit_price, pr.acquisition_price
FROM sales s
JOIN product_rank pr ON pr.rn = MOD(s.id, 25)
WHERE s.organization_id = @org_c_id AND MOD(s.id, 12) <> 0;

-- Org C bundle sales (every 12th sale)
INSERT INTO sale_items (sale_id, organization_id, product_id, bundle_id,
  quantity, list_unit_price_snapshot, discount_pct_snapshot, unit_price_snapshot, cost_price_snapshot)
SELECT s.id, @org_c_id, bi.product_id, @bun_c1,
  1,
  p.unit_price,
  5.00,
  ROUND(p.unit_price * 0.95, 2),
  p.acquisition_price
FROM sales s
JOIN bundle_items bi ON bi.bundle_id = @bun_c1 AND bi.organization_id = @org_c_id
JOIN products p ON p.id = bi.product_id
WHERE s.organization_id = @org_c_id AND MOD(s.id, 12) = 0;

-- ════════════════════════════════════════════════════════════════
-- 16. UPDATE SALES TOTALS from actual sale_items
-- ════════════════════════════════════════════════════════════════
UPDATE sales s
JOIN (
  SELECT sale_id,
    ROUND(SUM(quantity * unit_price_snapshot),      2) AS tot,
    ROUND(SUM(quantity * list_unit_price_snapshot), 2) AS sub
  FROM sale_items
  GROUP BY sale_id
) agg ON agg.sale_id = s.id
SET s.total_amount          = agg.tot,
    s.subtotal_amount       = agg.sub,
    s.discount_total_amount = ROUND(agg.sub - agg.tot, 2)
WHERE s.organization_id IN (@org_a_id, @org_b_id, @org_c_id);

-- ════════════════════════════════════════════════════════════════
-- 17. STOCK MOVEMENTS — SALE  (one per sale_item row)
-- ════════════════════════════════════════════════════════════════
INSERT INTO stock_movements (organization_id, product_id, delta, reason, notes, moved_at)
SELECT si.organization_id, si.product_id, -si.quantity, 'SALE',
  CONCAT('Venta #', si.sale_id),
  s.sold_at
FROM sale_items si
JOIN sales s ON s.id = si.sale_id
WHERE si.organization_id IN (@org_a_id, @org_b_id, @org_c_id);

-- ════════════════════════════════════════════════════════════════
-- 18. UPDATE current_quantity  (sum of all movements per product)
-- ════════════════════════════════════════════════════════════════
UPDATE products p
JOIN (
  SELECT product_id, organization_id, SUM(delta) AS total_delta
  FROM stock_movements
  GROUP BY product_id, organization_id
) mv ON mv.product_id = p.id AND mv.organization_id = p.organization_id
SET p.current_quantity = GREATEST(mv.total_delta, 0)
WHERE p.organization_id IN (@org_a_id, @org_b_id, @org_c_id);

-- ── Force realistic stock anomalies ──────────────────────────
-- Org A: one out-of-stock (high-velocity keyboard ran out), one overstock
UPDATE products SET current_quantity = 0
WHERE organization_id = @org_a_id AND sku = 'NVT-KBD-002';

UPDATE products SET current_quantity = 250
WHERE organization_id = @org_a_id AND sku = 'NVT-ACC-004';  -- overstock on niche filter

-- Org B: 3 overstocked slow movers, 2 critically low
UPDATE products SET current_quantity = 0 WHERE organization_id = @org_b_id AND sku = 'MID-FIL-004';
UPDATE products SET current_quantity = 3 WHERE organization_id = @org_b_id AND sku = 'MID-PRE-003';
UPDATE products SET current_quantity = 180 WHERE organization_id = @org_b_id AND sku = 'MID-PPR-001';
UPDATE products SET current_quantity = 220 WHERE organization_id = @org_b_id AND sku = 'MID-PPR-005';
UPDATE products SET current_quantity = 195 WHERE organization_id = @org_b_id AND sku = 'MID-FIL-001';

-- Org C: most products near zero or out of stock
UPDATE products SET current_quantity = 0 WHERE organization_id = @org_c_id AND sku IN
  ('ZRO-HOT-001','ZRO-HOT-003','ZRO-SNK-001','ZRO-SNK-003','ZRO-CRL-001','ZRO-BEV-001','ZRO-BEV-003','ZRO-CLN-002');
UPDATE products SET current_quantity = 2 WHERE organization_id = @org_c_id AND sku IN
  ('ZRO-HOT-002','ZRO-SNK-002','ZRO-CRL-002');
-- One overstock (bought wrong product)
UPDATE products SET current_quantity = 180 WHERE organization_id = @org_c_id AND sku = 'ZRO-BEV-004';

-- ════════════════════════════════════════════════════════════════
-- 19. STOCK ALERTS  (reflect final stock state + historical alerts)
-- ════════════════════════════════════════════════════════════════

-- Org A: ~120 historical alerts (85% resolved), few active
INSERT INTO stock_alerts (organization_id, product_id, alert_type, message, resolved, resolved_at, created_at)
WITH n0 AS (SELECT 0 v UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
            UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9),
nums AS (SELECT a.v*10+b.v AS n FROM n0 a, n0 b WHERE a.v*10+b.v BETWEEN 1 AND 24)
SELECT @org_a_id, p.id,
  CASE MOD(nums.n + p.id, 4)
    WHEN 0 THEN 'LOW_STOCK' WHEN 1 THEN 'PRICE_ANOMALY'
    WHEN 2 THEN 'LOW_STOCK' ELSE 'OUT_OF_STOCK'
  END,
  CONCAT('Alerta histórica producto ', p.sku),
  IF(MOD(nums.n, 6) < 5, 1, 0),   -- 83% resolved
  IF(MOD(nums.n, 6) < 5,
    DATE_ADD(DATE_SUB(NOW(), INTERVAL (730 - nums.n*29) DAY), INTERVAL 36 HOUR),
    NULL),
  DATE_SUB(NOW(), INTERVAL (730 - nums.n*29) DAY)
FROM nums, products p
WHERE p.organization_id = @org_a_id AND MOD(nums.n + p.id, 5) = 0;

-- Active alerts for Org A (current state)
INSERT INTO stock_alerts (organization_id, product_id, alert_type, message, resolved, created_at)
SELECT @org_a_id, id, 'LOW_STOCK',
  CONCAT('Stock bajo: ', sku, ' (', current_quantity, ' uds < mínimo ', minimum_stock_level, ')'),
  0, NOW()
FROM products
WHERE organization_id = @org_a_id AND current_quantity < minimum_stock_level AND current_quantity > 0;

INSERT INTO stock_alerts (organization_id, product_id, alert_type, message, resolved, created_at)
SELECT @org_a_id, id, 'OUT_OF_STOCK',
  CONCAT('Sin stock: ', sku),
  0, NOW()
FROM products
WHERE organization_id = @org_a_id AND current_quantity = 0;

INSERT INTO stock_alerts (organization_id, product_id, alert_type, message, resolved, created_at)
SELECT @org_a_id, id, 'OVERSTOCK',
  CONCAT('Sobrestock: ', sku, ' (', current_quantity, ' uds)'),
  0, NOW()
FROM products
WHERE organization_id = @org_a_id AND current_quantity > minimum_stock_level * 6;

-- Price anomaly alerts for Org A (5 products with large price swings)
INSERT INTO stock_alerts (organization_id, product_id, alert_type, message, resolved, created_at)
SELECT @org_a_id, id, 'PRICE_ANOMALY',
  CONCAT('Variación de precio detectada: ', sku),
  0, DATE_SUB(NOW(), INTERVAL MOD(id, 30) DAY)
FROM products
WHERE organization_id = @org_a_id
ORDER BY id LIMIT 5;

-- Org B: ~100 historical alerts (40% resolved), mix of overstock and low_stock
INSERT INTO stock_alerts (organization_id, product_id, alert_type, message, resolved, resolved_at, created_at)
WITH n0 AS (SELECT 0 v UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
            UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9),
nums AS (SELECT a.v*10+b.v AS n FROM n0 a, n0 b WHERE a.v*10+b.v BETWEEN 1 AND 20)
SELECT @org_b_id, p.id,
  CASE MOD(nums.n + p.id, 4)
    WHEN 0 THEN 'OVERSTOCK' WHEN 1 THEN 'LOW_STOCK'
    WHEN 2 THEN 'PRICE_ANOMALY' ELSE 'LOW_STOCK'
  END,
  CONCAT('Alerta histórica producto ', p.sku),
  IF(MOD(nums.n, 5) < 2, 1, 0),   -- 40% resolved
  IF(MOD(nums.n, 5) < 2,
    DATE_ADD(DATE_SUB(NOW(), INTERVAL (730 - nums.n*35) DAY), INTERVAL 72 HOUR),
    NULL),
  DATE_SUB(NOW(), INTERVAL (730 - nums.n*35) DAY)
FROM nums, products p
WHERE p.organization_id = @org_b_id AND MOD(nums.n + p.id, 5) = 0;

-- Active alerts for Org B
INSERT INTO stock_alerts (organization_id, product_id, alert_type, message, resolved, created_at)
SELECT @org_b_id, id, 'OUT_OF_STOCK', CONCAT('Sin stock: ', sku), 0, NOW()
FROM products WHERE organization_id = @org_b_id AND current_quantity = 0;

INSERT INTO stock_alerts (organization_id, product_id, alert_type, message, resolved, created_at)
SELECT @org_b_id, id, 'LOW_STOCK',
  CONCAT('Stock bajo: ', sku, ' (', current_quantity, ' uds)'),
  0, NOW()
FROM products WHERE organization_id = @org_b_id
  AND current_quantity > 0 AND current_quantity < minimum_stock_level;

INSERT INTO stock_alerts (organization_id, product_id, alert_type, message, resolved, created_at)
SELECT @org_b_id, id, 'OVERSTOCK',
  CONCAT('Sobrestock: ', sku, ' (', current_quantity, ' uds)'),
  0, NOW()
FROM products WHERE organization_id = @org_b_id
  AND current_quantity > minimum_stock_level * 5;

-- Org C: ~80 historical alerts (20% resolved), mostly OUT_OF_STOCK
INSERT INTO stock_alerts (organization_id, product_id, alert_type, message, resolved, resolved_at, created_at)
WITH n0 AS (SELECT 0 v UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
            UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9),
nums AS (SELECT a.v*10+b.v AS n FROM n0 a, n0 b WHERE a.v*10+b.v BETWEEN 1 AND 16)
SELECT @org_c_id, p.id,
  CASE MOD(nums.n + p.id, 5)
    WHEN 0 THEN 'OUT_OF_STOCK' WHEN 1 THEN 'OUT_OF_STOCK'
    WHEN 2 THEN 'LOW_STOCK'   WHEN 3 THEN 'PRICE_ANOMALY'
    ELSE 'OUT_OF_STOCK'
  END,
  CONCAT('Alerta histórica producto ', p.sku),
  IF(MOD(nums.n, 5) = 0, 1, 0),   -- only 20% resolved
  IF(MOD(nums.n, 5) = 0,
    DATE_ADD(DATE_SUB(NOW(), INTERVAL (730 - nums.n*44) DAY), INTERVAL 120 HOUR),
    NULL),
  DATE_SUB(NOW(), INTERVAL (730 - nums.n*44) DAY)
FROM nums, products p
WHERE p.organization_id = @org_c_id AND MOD(nums.n + p.id, 5) = 0;

-- Active alerts for Org C
INSERT INTO stock_alerts (organization_id, product_id, alert_type, message, resolved, created_at)
SELECT @org_c_id, id, 'OUT_OF_STOCK', CONCAT('Sin stock — sin fondos para reponer: ', sku), 0, NOW()
FROM products WHERE organization_id = @org_c_id AND current_quantity = 0;

INSERT INTO stock_alerts (organization_id, product_id, alert_type, message, resolved, created_at)
SELECT @org_c_id, id, 'LOW_STOCK',
  CONCAT('Stock crítico: ', sku, ' (', current_quantity, ' uds)'),
  0, NOW()
FROM products WHERE organization_id = @org_c_id
  AND current_quantity > 0 AND current_quantity < minimum_stock_level;

INSERT INTO stock_alerts (organization_id, product_id, alert_type, message, resolved, created_at)
SELECT @org_c_id, id, 'OVERSTOCK',
  CONCAT('Compra errónea — sobrestock: ', sku),
  0, NOW()
FROM products WHERE organization_id = @org_c_id
  AND current_quantity > minimum_stock_level * 5;

INSERT INTO stock_alerts (organization_id, product_id, alert_type, message, resolved, created_at)
SELECT @org_c_id, id, 'PRICE_ANOMALY',
  CONCAT('Precio por debajo de coste: ', sku),
  0, DATE_SUB(NOW(), INTERVAL MOD(id, 60) DAY)
FROM products WHERE organization_id = @org_c_id
ORDER BY id LIMIT 6;
