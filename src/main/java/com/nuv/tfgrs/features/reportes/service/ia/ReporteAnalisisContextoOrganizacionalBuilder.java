package com.nuv.tfgrs.features.reportes.service.ia;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReporteAnalisisContextoOrganizacionalBuilder {

    private static final int TOP_PRODUCTOS_LIMIT = 5;
    private static final int TOP_CLIENTES_LIMIT = 3;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ReporteAnalisisContextoOrganizacionalBuilder(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> build(Long organizationId) {
        MapSqlParameterSource params = new MapSqlParameterSource("organizationId", organizationId);

        Map<String, Object> contexto = new LinkedHashMap<>();
        contexto.put("inventario", inventario(params));
        contexto.put("alertas", alertas(params));
        contexto.put("clientes", clientes(params));
        contexto.put("movimientos", movimientos(params));
        contexto.put("ventas", ventas(params));
        contexto.put("tendenciaMensualVentas", tendenciaMensualVentas(params));
        contexto.put("topProductos", topProductos(params));
        contexto.put("topClientes", topClientes(params));
        return contexto;
    }

    private Map<String, Object> inventario(MapSqlParameterSource params) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        SELECT
                          COUNT(*) AS totalProductos,
                          COALESCE(SUM(current_quantity), 0) AS unidadesTotales,
                          COALESCE(SUM(unit_price * current_quantity), 0) AS valorInventario,
                          SUM(CASE WHEN current_quantity < minimum_stock_level THEN 1 ELSE 0 END) AS productosStockBajo
                        FROM products
                        WHERE organization_id = :organizationId
                        """,
                params
        );
        return Map.of(
                "totalProductos", toLong(row.get("totalProductos")),
                "unidadesTotales", toLong(row.get("unidadesTotales")),
                "valorInventario", round2(row.get("valorInventario")),
                "productosStockBajo", toLong(row.get("productosStockBajo"))
        );
    }

    private Map<String, Object> alertas(MapSqlParameterSource params) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        SELECT
                          COUNT(*) AS totalAlertas,
                          SUM(CASE WHEN resolved = false THEN 1 ELSE 0 END) AS pendientes,
                          SUM(CASE WHEN resolved = true THEN 1 ELSE 0 END) AS resueltas
                        FROM stock_alerts
                        WHERE organization_id = :organizationId
                        """,
                params
        );
        return Map.of(
                "totalAlertas", toLong(row.get("totalAlertas")),
                "pendientes", toLong(row.get("pendientes")),
                "resueltas", toLong(row.get("resueltas"))
        );
    }

    private Map<String, Object> clientes(MapSqlParameterSource params) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        SELECT
                          COUNT(*) AS totalClientes,
                          SUM(CASE WHEN is_active = true THEN 1 ELSE 0 END) AS activos,
                          SUM(CASE WHEN is_active = false THEN 1 ELSE 0 END) AS archivados,
                          SUM(CASE WHEN created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 ELSE 0 END) AS nuevos30d
                        FROM clients
                        WHERE organization_id = :organizationId
                        """,
                params
        );
        return Map.of(
                "totalClientes", toLong(row.get("totalClientes")),
                "activos", toLong(row.get("activos")),
                "archivados", toLong(row.get("archivados")),
                "nuevos30d", toLong(row.get("nuevos30d"))
        );
    }

    private Map<String, Object> movimientos(MapSqlParameterSource params) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        SELECT
                          COUNT(*) AS totalMovimientos,
                          SUM(CASE WHEN delta > 0 THEN 1 ELSE 0 END) AS entradas,
                          SUM(CASE WHEN delta < 0 THEN 1 ELSE 0 END) AS salidas,
                          SUM(CASE WHEN moved_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 ELSE 0 END) AS ultimos30d
                        FROM stock_movements
                        WHERE organization_id = :organizationId
                        """,
                params
        );
        return Map.of(
                "totalMovimientos", toLong(row.get("totalMovimientos")),
                "entradas", toLong(row.get("entradas")),
                "salidas", toLong(row.get("salidas")),
                "ultimos30d", toLong(row.get("ultimos30d"))
        );
    }

    private Map<String, Object> ventas(MapSqlParameterSource params) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        SELECT
                          COUNT(*) AS totalVentas,
                          COALESCE(SUM(total_amount), 0) AS totalIngresos,
                          COALESCE(AVG(total_amount), 0) AS ticketPromedio,
                          SUM(CASE WHEN sold_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 ELSE 0 END) AS ventas30d,
                          COALESCE(SUM(CASE WHEN sold_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN total_amount ELSE 0 END), 0) AS ingresos30d
                        FROM sales
                        WHERE organization_id = :organizationId
                        """,
                params
        );
        return Map.of(
                "totalVentas", toLong(row.get("totalVentas")),
                "totalIngresos", round2(row.get("totalIngresos")),
                "ticketPromedio", round2(row.get("ticketPromedio")),
                "ventas30d", toLong(row.get("ventas30d")),
                "ingresos30d", round2(row.get("ingresos30d"))
        );
    }

    private List<Map<String, Object>> tendenciaMensualVentas(MapSqlParameterSource params) {
        return jdbcTemplate.queryForList(
                """
                        SELECT
                          DATE_FORMAT(sold_at, '%%Y-%%m') AS mes,
                          COUNT(*) AS totalVentas,
                          ROUND(COALESCE(SUM(total_amount), 0), 2) AS ingresos
                        FROM sales
                        WHERE organization_id = :organizationId
                          AND sold_at >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH)
                        GROUP BY DATE_FORMAT(sold_at, '%%Y-%%m')
                        ORDER BY mes DESC
                        LIMIT 6
                        """,
                params
        ).stream().map(row -> Map.of(
                "mes", row.get("mes"),
                "totalVentas", toLong(row.get("totalVentas")),
                "ingresos", round2(row.get("ingresos"))
        )).toList();
    }

    private List<Map<String, Object>> topProductos(MapSqlParameterSource params) {
        return jdbcTemplate.queryForList(
                """
                        SELECT
                          p.name AS nombre,
                          SUM(si.quantity) AS unidades,
                          ROUND(SUM(si.unit_price_snapshot * si.quantity), 2) AS ingresos
                        FROM sale_items si
                        JOIN sales s ON s.id = si.sale_id
                        JOIN products p ON p.id = si.product_id
                        WHERE s.organization_id = :organizationId
                          AND p.organization_id = :organizationId
                        GROUP BY si.product_id, p.name
                        ORDER BY ingresos DESC
                        LIMIT %d
                        """.formatted(TOP_PRODUCTOS_LIMIT),
                params
        ).stream().map(row -> Map.of(
                "nombre", row.get("nombre"),
                "unidades", toLong(row.get("unidades")),
                "ingresos", round2(row.get("ingresos"))
        )).toList();
    }

    private List<Map<String, Object>> topClientes(MapSqlParameterSource params) {
        return jdbcTemplate.queryForList(
                """
                        SELECT
                          c.name AS nombre,
                          COUNT(s.id) AS totalVentas,
                          ROUND(SUM(s.total_amount), 2) AS totalGastado
                        FROM sales s
                        JOIN clients c ON c.id = s.client_id
                        WHERE s.organization_id = :organizationId
                          AND c.organization_id = :organizationId
                        GROUP BY s.client_id, c.name
                        ORDER BY totalGastado DESC
                        LIMIT %d
                        """.formatted(TOP_CLIENTES_LIMIT),
                params
        ).stream().map(row -> Map.of(
                "nombre", row.get("nombre"),
                "totalVentas", toLong(row.get("totalVentas")),
                "totalGastado", round2(row.get("totalGastado"))
        )).toList();
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number number) return number.longValue();
        return 0L;
    }

    private double round2(Object value) {
        if (!(value instanceof Number number)) return 0.0;
        return Math.round(number.doubleValue() * 100.0) / 100.0;
    }
}
