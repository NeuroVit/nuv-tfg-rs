-- Corrige el significado de valorTotalInventario en reportes de inventario:
-- debe representar el valor potencial de venta del stock actual (unit_price * current_quantity),
-- no el coste de adquisición.

UPDATE reportes r
SET r.datos = JSON_SET(
    COALESCE(r.datos, JSON_OBJECT()),
    '$.valorTotalInventario',
    (
        SELECT ROUND(
            COALESCE(SUM(COALESCE(p.unit_price, 0) * COALESCE(p.current_quantity, 0)), 0),
            2
        )
        FROM products p
        WHERE p.organization_id = r.organization_id
    ),
    '$.generadoEn',
    DATE_FORMAT(NOW(), '%Y-%m-%dT%H:%i:%s')
)
WHERE r.tipo = 'INVENTARIO';
