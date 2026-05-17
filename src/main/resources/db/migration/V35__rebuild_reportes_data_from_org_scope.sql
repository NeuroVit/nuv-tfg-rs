-- Recalcula reportes históricos usando datos reales por organización.
-- Aplica a todos los reportes existentes, incluyendo los creados antes
-- del scope multitenant en reportes.

-- INVENTARIO
UPDATE reportes r
SET r.datos = JSON_OBJECT(
    'generadoEn', DATE_FORMAT(NOW(), '%Y-%m-%dT%H:%i:%s'),
    'totalProductos', (
        SELECT COUNT(*)
        FROM products p
        WHERE p.organization_id = r.organization_id
    ),
    'valorTotalInventario', (
        SELECT ROUND(
            COALESCE(SUM(COALESCE(p.acquisition_price, 0) * COALESCE(p.current_quantity, 0)), 0),
            2
        )
        FROM products p
        WHERE p.organization_id = r.organization_id
    ),
    'productosStockBajo', (
        SELECT COUNT(*)
        FROM products p
        WHERE p.organization_id = r.organization_id
          AND p.current_quantity < p.minimum_stock_level
    ),
    'resumenStockBajo', COALESCE((
        SELECT JSON_ARRAYAGG(
            JSON_OBJECT(
                'nombre', p.name,
                'sku', p.sku,
                'stockActual', p.current_quantity,
                'stockMinimo', p.minimum_stock_level
            )
        )
        FROM products p
        WHERE p.organization_id = r.organization_id
          AND p.current_quantity < p.minimum_stock_level
    ), JSON_ARRAY())
)
WHERE r.tipo = 'INVENTARIO';

-- ALERTAS
UPDATE reportes r
SET r.datos = JSON_OBJECT(
    'generadoEn', DATE_FORMAT(NOW(), '%Y-%m-%dT%H:%i:%s'),
    'totalAlertas', (
        SELECT COUNT(*)
        FROM stock_alerts sa
        WHERE sa.organization_id = r.organization_id
    ),
    'alertasPendientes', (
        SELECT COUNT(*)
        FROM stock_alerts sa
        WHERE sa.organization_id = r.organization_id
          AND sa.resolved = 0
    ),
    'alertasResueltas', (
        SELECT COUNT(*)
        FROM stock_alerts sa
        WHERE sa.organization_id = r.organization_id
          AND sa.resolved = 1
    ),
    'detalle', COALESCE((
        SELECT JSON_ARRAYAGG(
            JSON_OBJECT(
                'producto', COALESCE(p.name, '—'),
                'sku', COALESCE(p.sku, '—'),
                'tipo', COALESCE(sa.alert_type, 'LOW_STOCK')
            )
        )
        FROM stock_alerts sa
        LEFT JOIN products p ON p.id = sa.product_id
        WHERE sa.organization_id = r.organization_id
          AND sa.resolved = 0
    ), JSON_ARRAY())
)
WHERE r.tipo = 'ALERTAS';

-- CLIENTES
UPDATE reportes r
SET r.datos = JSON_OBJECT(
    'generadoEn', DATE_FORMAT(NOW(), '%Y-%m-%dT%H:%i:%s'),
    'totalClientes', (
        SELECT COUNT(*)
        FROM clients c
        WHERE c.organization_id = r.organization_id
    ),
    'clientesActivos', (
        SELECT COUNT(*)
        FROM clients c
        WHERE c.organization_id = r.organization_id
          AND c.is_active = 1
    ),
    'clientesArchivados', (
        SELECT COUNT(*)
        FROM clients c
        WHERE c.organization_id = r.organization_id
          AND c.is_active = 0
    ),
    'nuevosUltimos30Dias', (
        SELECT COUNT(*)
        FROM clients c
        WHERE c.organization_id = r.organization_id
          AND c.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
    ),
    'listado', COALESCE((
        SELECT JSON_ARRAYAGG(
            JSON_OBJECT(
                'nombre', c.name,
                'email', COALESCE(c.email, '—'),
                'activo', IF(c.is_active = 1, TRUE, FALSE)
            )
        )
        FROM clients c
        WHERE c.organization_id = r.organization_id
    ), JSON_ARRAY())
)
WHERE r.tipo = 'CLIENTES';

-- MOVIMIENTOS
UPDATE reportes r
SET r.datos = JSON_OBJECT(
    'generadoEn', DATE_FORMAT(NOW(), '%Y-%m-%dT%H:%i:%s'),
    'totalMovimientos', (
        SELECT COUNT(*)
        FROM stock_movements sm
        WHERE sm.organization_id = r.organization_id
    ),
    'entradas', (
        SELECT COUNT(*)
        FROM stock_movements sm
        WHERE sm.organization_id = r.organization_id
          AND sm.delta > 0
    ),
    'salidas', (
        SELECT COUNT(*)
        FROM stock_movements sm
        WHERE sm.organization_id = r.organization_id
          AND sm.delta < 0
    )
)
WHERE r.tipo = 'MOVIMIENTOS';

-- VENTAS
UPDATE reportes r
SET r.datos = JSON_OBJECT(
    'generadoEn', DATE_FORMAT(NOW(), '%Y-%m-%dT%H:%i:%s'),
    'totalVentas', (
        SELECT COUNT(*)
        FROM sales s
        WHERE s.organization_id = r.organization_id
    ),
    'totalIngresos', (
        SELECT COALESCE(SUM(s.total_amount), 0)
        FROM sales s
        WHERE s.organization_id = r.organization_id
    ),
    'promedioVenta', (
        SELECT ROUND(COALESCE(AVG(s.total_amount), 0), 2)
        FROM sales s
        WHERE s.organization_id = r.organization_id
    ),
    'ventasMes', (
        SELECT COUNT(*)
        FROM sales s
        WHERE s.organization_id = r.organization_id
          AND YEAR(s.sold_at) = YEAR(NOW())
          AND MONTH(s.sold_at) = MONTH(NOW())
    ),
    'ingresosMes', (
        SELECT COALESCE(SUM(s.total_amount), 0)
        FROM sales s
        WHERE s.organization_id = r.organization_id
          AND YEAR(s.sold_at) = YEAR(NOW())
          AND MONTH(s.sold_at) = MONTH(NOW())
    ),
    'nuevosClientes30Dias', (
        SELECT COUNT(*)
        FROM clients c
        WHERE c.organization_id = r.organization_id
          AND c.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
    ),
    'topProductos', COALESCE((
        SELECT JSON_ARRAYAGG(
            JSON_OBJECT(
                'nombre', t.nombre,
                'totalUnidades', t.totalUnidades,
                'ingresos', t.ingresos
            )
        )
        FROM (
            SELECT
                p.name AS nombre,
                SUM(si.quantity) AS totalUnidades,
                ROUND(SUM(si.unit_price_snapshot * si.quantity), 2) AS ingresos
            FROM sale_items si
            JOIN sales s ON s.id = si.sale_id
            JOIN products p ON p.id = si.product_id
            WHERE s.organization_id = r.organization_id
              AND p.organization_id = r.organization_id
            GROUP BY si.product_id, p.name
            ORDER BY ingresos DESC
            LIMIT 5
        ) t
    ), JSON_ARRAY()),
    'topClientes', COALESCE((
        SELECT JSON_ARRAYAGG(
            JSON_OBJECT(
                'nombre', t.nombre,
                'totalVentas', t.totalVentas,
                'totalGastado', t.totalGastado
            )
        )
        FROM (
            SELECT
                c.name AS nombre,
                COUNT(s.id) AS totalVentas,
                ROUND(SUM(s.total_amount), 2) AS totalGastado
            FROM sales s
            JOIN clients c ON c.id = s.client_id
            WHERE s.organization_id = r.organization_id
              AND c.organization_id = r.organization_id
            GROUP BY s.client_id, c.name
            ORDER BY totalGastado DESC
            LIMIT 3
        ) t
    ), JSON_ARRAY())
)
WHERE r.tipo = 'VENTAS';
