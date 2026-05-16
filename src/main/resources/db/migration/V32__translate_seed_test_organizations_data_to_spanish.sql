-- Translate seeded test organization data to Spanish for databases
-- where V23 was already applied before the seed text was localized.

-- 1) Organizations
UPDATE organizations
SET name = CASE name
  WHEN 'NovaTech Pro' THEN 'NovaTech Profesional'
  WHEN 'MidMart Supply' THEN 'Suministros MidMart'
  WHEN 'ZeroEdge Trading' THEN 'Comercial ZeroEdge'
  ELSE name
END
WHERE slug IN ('novatech-pro', 'midmart-supply', 'zeroedge-trading');

-- 2) Seed users (last name labels)
UPDATE users
SET apellidos = CASE username
  WHEN 'nova_admin' THEN 'Administrador'
  WHEN 'nova_manager' THEN 'Gerente'
  WHEN 'mid_admin' THEN 'Administrador'
  WHEN 'mid_manager' THEN 'Gerente'
  WHEN 'zero_admin' THEN 'Administrador'
  WHEN 'zero_manager' THEN 'Gerente'
  ELSE apellidos
END
WHERE username IN (
  'nova_admin', 'nova_manager', 'mid_admin', 'mid_manager', 'zero_admin', 'zero_manager'
);

-- 3) Categories
UPDATE categories c
JOIN organizations o ON o.id = c.organization_id
SET
  c.name = CASE c.name
    WHEN 'Monitors' THEN 'Monitores'
    WHEN 'Keyboards & Mice' THEN 'Teclados y Ratones'
    WHEN 'Headsets & Audio' THEN 'Auriculares y Audio'
    WHEN 'Cables & Adapters' THEN 'Cables y Adaptadores'
    WHEN 'Accessories' THEN 'Accesorios'
    WHEN 'Paper & Printing' THEN 'Papel e Impresión'
    WHEN 'Pens & Writing' THEN 'Bolígrafos y Escritura'
    WHEN 'Filing & Storage' THEN 'Archivo y Almacenaje'
    WHEN 'Desk Accessories' THEN 'Accesorios de Escritorio'
    WHEN 'Presentation' THEN 'Presentación'
    WHEN 'Hot Drinks' THEN 'Bebidas Calientes'
    WHEN 'Snacks & Sweets' THEN 'Aperitivos y Dulces'
    WHEN 'Cereals & Breakfast' THEN 'Cereales y Desayuno'
    WHEN 'Cleaning' THEN 'Limpieza'
    WHEN 'Beverages' THEN 'Bebidas'
    ELSE c.name
  END,
  c.description = CASE c.description
    WHEN 'Displays and screens for workstations' THEN 'Pantallas para puestos de trabajo'
    WHEN 'Input peripherals and combos' THEN 'Periféricos de entrada y combos'
    WHEN 'Headphones, microphones and webcams' THEN 'Auriculares, micrófonos y cámaras web'
    WHEN 'Connectivity cables, hubs and docks' THEN 'Cables de conectividad, concentradores y bases'
    WHEN 'Stands, arms and ergonomic add-ons' THEN 'Soportes, brazos y extras ergonómicos'
    WHEN 'Paper, toner and printing consumables' THEN 'Papel, tóner y consumibles de impresión'
    WHEN 'Ballpens, markers and correction supplies' THEN 'Bolígrafos, rotuladores y correctores'
    WHEN 'Binders, folders and filing systems' THEN 'Archivadores, carpetas y sistemas de archivo'
    WHEN 'Staplers, tape and everyday desk items' THEN 'Grapadoras, cinta y artículos de escritorio'
    WHEN 'Whiteboards, easels and remote controls' THEN 'Pizarras, caballetes y mandos de presentación'
    WHEN 'Coffee, tea and hot beverage mixes' THEN 'Café, té y preparados de bebidas calientes'
    WHEN 'Crisps, chocolate and on-the-go snacks' THEN 'Patatas, chocolate y aperitivos para llevar'
    WHEN 'Oats, cereals and breakfast staples' THEN 'Avena, cereales y básicos de desayuno'
    WHEN 'Household cleaning products' THEN 'Productos de limpieza para el hogar'
    WHEN 'Cold drinks, juices and plant milks' THEN 'Bebidas frías, zumos y bebidas vegetales'
    ELSE c.description
  END
WHERE o.slug IN ('novatech-pro', 'midmart-supply', 'zeroedge-trading');

-- 4) Providers
UPDATE providers p
JOIN organizations o ON o.id = p.organization_id
SET
  p.name = CASE p.name
    WHEN 'PeriphPlus' THEN 'PerifPlus'
    WHEN 'CableWorld' THEN 'MundoCable'
    WHEN 'OfficeDepot ES' THEN 'Suministros Oficina ES'
    WHEN 'PaperLane' THEN 'RutaPapel'
    WHEN 'QuickFast Foods' THEN 'Alimentos Rápidos'
    ELSE p.name
  END,
  p.email = CASE p.email
    WHEN 'orders@techdistrib.test' THEN 'pedidos@techdistrib.test'
    WHEN 'stock@periphplus.test' THEN 'inventario@periphplus.test'
    WHEN 'sales@cableworld.test' THEN 'ventas@cableworld.test'
    WHEN 'orders@quickfast.test' THEN 'pedidos@quickfast.test'
    ELSE p.email
  END,
  p.warehouse_location = CASE p.warehouse_location
    WHEN 'Madrid Logistics Hub' THEN 'Centro Logístico Madrid'
    WHEN 'Barcelona Warehouse' THEN 'Almacén Barcelona'
    WHEN 'Valencia Distribution' THEN 'Distribución Valencia'
    WHEN 'Bilbao Supply Centre' THEN 'Centro de Suministro Bilbao'
    WHEN 'Sevilla Depot' THEN 'Depósito Sevilla'
    WHEN 'Zaragoza Cash & Carry' THEN 'Mayorista Zaragoza'
    ELSE p.warehouse_location
  END
WHERE o.slug IN ('novatech-pro', 'midmart-supply', 'zeroedge-trading');

-- 5) Products (by SKU)
UPDATE products p
JOIN organizations o ON o.id = p.organization_id
SET p.name = CASE p.sku
  WHEN 'NVT-MON-001' THEN 'Monitor Ultrapanorámico 34"'
  WHEN 'NVT-MON-002' THEN 'Pantalla 4K 27"'
  WHEN 'NVT-MON-003' THEN 'Monitor Full HD 24"'
  WHEN 'NVT-MON-004' THEN 'Monitor Portátil 15"'
  WHEN 'NVT-MON-005' THEN 'Monitor Curvo 27"'
  WHEN 'NVT-KBD-001' THEN 'Teclado Mecánico RGB'
  WHEN 'NVT-KBD-002' THEN 'Teclado Inalámbrico'
  WHEN 'NVT-KBD-003' THEN 'Ratón para Juegos 16000DPI'
  WHEN 'NVT-KBD-004' THEN 'Ratón Inalámbrico'
  WHEN 'NVT-KBD-005' THEN 'Combo Teclado y Ratón'
  WHEN 'NVT-AUD-001' THEN 'Auriculares USB-C'
  WHEN 'NVT-AUD-002' THEN 'Auriculares Bluetooth'
  WHEN 'NVT-AUD-003' THEN 'Micrófono de Estudio'
  WHEN 'NVT-AUD-004' THEN 'Cámara web 1080p'
  WHEN 'NVT-AUD-005' THEN 'Auriculares con Cancelación de Ruido'
  WHEN 'NVT-CAB-001' THEN 'Concentrador USB-C 7 Puertos'
  WHEN 'NVT-CAB-002' THEN 'Cable HDMI 2m'
  WHEN 'NVT-CAB-003' THEN 'USB-C a DisplayPort'
  WHEN 'NVT-CAB-004' THEN 'Base Thunderbolt 4'
  WHEN 'NVT-CAB-005' THEN 'Batería Externa 20000mAh'
  WHEN 'NVT-ACC-001' THEN 'Soporte de Portátil Aluminio'
  WHEN 'NVT-ACC-002' THEN 'Brazo Doble para Monitor'
  WHEN 'NVT-ACC-003' THEN 'Kit de Gestión de Cables'
  WHEN 'NVT-ACC-004' THEN 'Filtro de Pantalla Antirreflejo'
  WHEN 'NVT-ACC-005' THEN 'Reposamuñecas Ergonómico'
  WHEN 'MID-PPR-001' THEN 'Resma Papel A4 500h'
  WHEN 'MID-PPR-002' THEN 'Papel Fotográfico Premium A4'
  WHEN 'MID-PPR-003' THEN 'Tóner Láser Negro'
  WHEN 'MID-PPR-004' THEN 'Paquete Múltiple Tinta Impresora'
  WHEN 'MID-PPR-005' THEN 'Papel Carbón'
  WHEN 'MID-PEN-001' THEN 'Caja de Bolígrafos 50'
  WHEN 'MID-PEN-002' THEN 'Set de Bolígrafo Líquido 10'
  WHEN 'MID-PEN-003' THEN 'Set de Subrayadores 8 Col'
  WHEN 'MID-PEN-004' THEN 'Rotuladores Permanentes 12 ud'
  WHEN 'MID-PEN-005' THEN 'Cinta Correctora 5 ud'
  WHEN 'MID-FIL-001' THEN 'Archivador de Anillas A4 50mm'
  WHEN 'MID-FIL-002' THEN 'Caja Carpetas Colgantes 50'
  WHEN 'MID-FIL-003' THEN 'Carpetas Plásticas 50 ud'
  WHEN 'MID-FIL-004' THEN 'Archivador 3 Cajones'
  WHEN 'MID-FIL-005' THEN 'Fundas para Documentos 100 ud'
  WHEN 'MID-DSK-001' THEN 'Grapadora 30 Hojas'
  WHEN 'MID-DSK-002' THEN 'Grapas 5000 ud'
  WHEN 'MID-DSK-003' THEN 'Dispensador de Cinta + Rollos'
  WHEN 'MID-DSK-004' THEN 'Tijeras Oficina Profesional'
  WHEN 'MID-DSK-005' THEN 'Notas Adhesivas Paquete 6'
  WHEN 'MID-PRE-001' THEN 'Pizarra Blanca 90x60cm'
  WHEN 'MID-PRE-002' THEN 'Rotuladores Pizarra 8 ud'
  WHEN 'MID-PRE-003' THEN 'Caballete para Bloc'
  WHEN 'MID-PRE-004' THEN 'Mando para Presentaciones'
  WHEN 'MID-PRE-005' THEN 'Blocs para Caballete 2 ud'
  WHEN 'ZRO-HOT-001' THEN 'Café Molido 500g'
  WHEN 'ZRO-HOT-002' THEN 'Café Soluble 200g'
  WHEN 'ZRO-HOT-003' THEN 'Bolsitas de Té 80 ud'
  WHEN 'ZRO-HOT-004' THEN 'Preparado Chocolate Caliente 500g'
  WHEN 'ZRO-HOT-005' THEN 'Infusiones Surtidas 40 ud'
  WHEN 'ZRO-SNK-001' THEN 'Patatas Fritas 150g'
  WHEN 'ZRO-SNK-002' THEN 'Frutos Secos Mixtos 200g'
  WHEN 'ZRO-SNK-003' THEN 'Tableta de Chocolate 100g'
  WHEN 'ZRO-SNK-004' THEN 'Tortitas de Arroz 130g'
  WHEN 'ZRO-SNK-005' THEN 'Barritas de Granola 6 ud'
  WHEN 'ZRO-CRL-001' THEN 'Copos de Avena 1kg'
  WHEN 'ZRO-CRL-002' THEN 'Copos de Maíz 750g'
  WHEN 'ZRO-CRL-003' THEN 'Muesli 500g'
  WHEN 'ZRO-CRL-004' THEN 'Preparado para Tortitas 500g'
  WHEN 'ZRO-CRL-005' THEN 'Granola Proteica 400g'
  WHEN 'ZRO-CLN-001' THEN 'Spray Multiusos 500ml'
  WHEN 'ZRO-CLN-002' THEN 'Lavavajillas Líquido 500ml'
  WHEN 'ZRO-CLN-003' THEN 'Limpiador de Suelos 1L'
  WHEN 'ZRO-CLN-004' THEN 'Bolsas de Basura 30 ud'
  WHEN 'ZRO-CLN-005' THEN 'Esponjas 10 ud'
  WHEN 'ZRO-BEV-001' THEN 'Agua con Gas 1.5L'
  WHEN 'ZRO-BEV-002' THEN 'Zumo de Naranja 1L'
  WHEN 'ZRO-BEV-003' THEN 'Bebida de Avena 1L'
  WHEN 'ZRO-BEV-004' THEN 'Bebida Isotónica 500ml'
  WHEN 'ZRO-BEV-005' THEN 'Batido 330ml'
  ELSE p.name
END
WHERE o.slug IN ('novatech-pro', 'midmart-supply', 'zeroedge-trading')
  AND p.sku IN (
    'NVT-MON-001','NVT-MON-002','NVT-MON-003','NVT-MON-004','NVT-MON-005',
    'NVT-KBD-001','NVT-KBD-002','NVT-KBD-003','NVT-KBD-004','NVT-KBD-005',
    'NVT-AUD-001','NVT-AUD-002','NVT-AUD-003','NVT-AUD-004','NVT-AUD-005',
    'NVT-CAB-001','NVT-CAB-002','NVT-CAB-003','NVT-CAB-004','NVT-CAB-005',
    'NVT-ACC-001','NVT-ACC-002','NVT-ACC-003','NVT-ACC-004','NVT-ACC-005',
    'MID-PPR-001','MID-PPR-002','MID-PPR-003','MID-PPR-004','MID-PPR-005',
    'MID-PEN-001','MID-PEN-002','MID-PEN-003','MID-PEN-004','MID-PEN-005',
    'MID-FIL-001','MID-FIL-002','MID-FIL-003','MID-FIL-004','MID-FIL-005',
    'MID-DSK-001','MID-DSK-002','MID-DSK-003','MID-DSK-004','MID-DSK-005',
    'MID-PRE-001','MID-PRE-002','MID-PRE-003','MID-PRE-004','MID-PRE-005',
    'ZRO-HOT-001','ZRO-HOT-002','ZRO-HOT-003','ZRO-HOT-004','ZRO-HOT-005',
    'ZRO-SNK-001','ZRO-SNK-002','ZRO-SNK-003','ZRO-SNK-004','ZRO-SNK-005',
    'ZRO-CRL-001','ZRO-CRL-002','ZRO-CRL-003','ZRO-CRL-004','ZRO-CRL-005',
    'ZRO-CLN-001','ZRO-CLN-002','ZRO-CLN-003','ZRO-CLN-004','ZRO-CLN-005',
    'ZRO-BEV-001','ZRO-BEV-002','ZRO-BEV-003','ZRO-BEV-004','ZRO-BEV-005'
  );

-- 6) Clients
UPDATE clients c
JOIN organizations o ON o.id = c.organization_id
SET
  c.name = CASE c.name
    WHEN 'Acme Corp' THEN 'Acme Corporación'
    WHEN 'BlueSky Agency' THEN 'Agencia CieloAzul'
    WHEN 'Vertex Studios' THEN 'Estudios Vertex'
    WHEN 'Nexum IT' THEN 'Nexum Informática'
    WHEN 'Orbit Media' THEN 'Medios Órbita'
    WHEN 'Crest Analytics' THEN 'Analítica Crest'
    WHEN 'Pinnacle Design' THEN 'Diseño Cúspide'
    WHEN 'Apex Consulting' THEN 'Consultoría Apex'
    WHEN 'ZenSoft Solutions' THEN 'Soluciones ZenSoft'
    WHEN 'Maple Startups' THEN 'Emprendimientos Maple'
    WHEN 'Ironclad Security' THEN 'Seguridad Ironclad'
    WHEN 'ClearPath Admin' THEN 'Administración RutaClara'
    WHEN 'Redwood Office' THEN 'Oficina Redwood'
    WHEN 'Goldstar Notary' THEN 'Notaría Goldstar'
    WHEN 'Birch Partners' THEN 'Socios Birch'
    WHEN 'Cedarwood Assoc' THEN 'Asoc. Cedarwood'
    WHEN 'Silverthorn Law' THEN 'Bufete Silverthorn'
    WHEN 'NorthPeak HR' THEN 'RRHH NorthPeak'
    WHEN 'Crossroads Print' THEN 'Impresión Crossroads'
    WHEN 'BlueLine Services' THEN 'Servicios BlueLine'
    WHEN 'Harbour Estates' THEN 'Inmuebles Harbour'
    WHEN 'Casa Martinez' THEN 'Casa Martínez'
    WHEN 'Mini Market Lola' THEN 'Minimercado Lola'
    ELSE c.name
  END,
  c.email = CASE c.email
    WHEN 'admin@bluesky.test' THEN 'administracion@bluesky.test'
    WHEN 'office@vertex.test' THEN 'oficina@vertex.test'
    WHEN 'stock@nexum.test' THEN 'inventario@nexum.test'
    WHEN 'orders@crest.test' THEN 'pedidos@crest.test'
    WHEN 'supplies@pinnacle.test' THEN 'suministros@pinnacle.test'
    WHEN 'procurement@apex.test' THEN 'compras@apex.test'
    WHEN 'it@zensoft.test' THEN 'informatica@zensoft.test'
    WHEN 'office@maple.test' THEN 'oficina@maple.test'
    WHEN 'orders@ironclad.test' THEN 'pedidos@ironclad.test'
    WHEN 'admin@redwood.test' THEN 'administracion@redwood.test'
    WHEN 'office@cedarwood.test' THEN 'oficina@cedarwood.test'
    WHEN 'admin@silverthorn.test' THEN 'administracion@silverthorn.test'
    WHEN 'orders@crossroads.test' THEN 'pedidos@crossroads.test'
    WHEN 'office@harbour.test' THEN 'oficina@harbour.test'
    WHEN 'stock@elrincon.test' THEN 'inventario@elrincon.test'
    WHEN 'orders@mmlola.test' THEN 'pedidos@mmlola.test'
    WHEN 'admin@despachorapido.test' THEN 'administracion@despachorapido.test'
    ELSE c.email
  END
WHERE o.slug IN ('novatech-pro', 'midmart-supply', 'zeroedge-trading');

-- 7) Bundles
UPDATE bundles b
JOIN organizations o ON o.id = b.organization_id
SET
  b.name = CASE b.name
    WHEN 'Office Setup Kit' THEN 'Kit de Oficina'
    WHEN 'Gamer Bundle' THEN 'Lote para Juegos'
    WHEN 'Studio Creator Kit' THEN 'Kit Creador Estudio'
    WHEN 'Travel Pack' THEN 'Lote de Viaje'
    WHEN 'Desk Starter' THEN 'Lote Escritorio'
    WHEN 'Filing Kit' THEN 'Kit Archivo'
    WHEN 'Budget Breakfast' THEN 'Desayuno Económico'
    ELSE b.name
  END,
  b.description = CASE b.description
    WHEN 'Monitor + keyboard + webcam combo' THEN 'Monitor + teclado + cámara web'
    WHEN 'Gaming mouse + RGB keyboard + headset' THEN 'Ratón para juegos + teclado RGB + auriculares'
    WHEN 'Webcam + mic + USB hub' THEN 'Cámara web + micrófono + concentrador USB'
    WHEN 'Portable monitor + laptop stand + USB-C hub' THEN 'Monitor portátil + soporte portátil + concentrador USB-C'
    WHEN 'Paper + pens + stapler combo' THEN 'Papel + bolígrafos + grapadora'
    WHEN 'Binders + folders + suspension files' THEN 'Archivadores + carpetas + carpetas colgantes'
    WHEN 'Oats + tea + coffee starter deal' THEN 'Avena + té + café'
    ELSE b.description
  END
WHERE o.slug IN ('novatech-pro', 'midmart-supply', 'zeroedge-trading');

-- 8) Stock movement note
UPDATE stock_movements sm
JOIN organizations o ON o.id = sm.organization_id
SET sm.notes = 'Stock inicial'
WHERE o.slug IN ('novatech-pro', 'midmart-supply', 'zeroedge-trading')
  AND sm.reason = 'INITIAL'
  AND sm.notes = 'Opening stock';
