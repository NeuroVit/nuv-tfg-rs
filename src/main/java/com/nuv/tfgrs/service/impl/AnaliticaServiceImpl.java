package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.dto.response.BundleCandidateRowResponse;
import com.nuv.tfgrs.dto.response.FinanzasResumenResponse;
import com.nuv.tfgrs.dto.response.InventorySummaryRowResponse;
import com.nuv.tfgrs.dto.response.PriceTrendRowResponse;
import com.nuv.tfgrs.dto.response.RentabilidadProductoRowResponse;
import com.nuv.tfgrs.dto.response.SalesVelocityRowResponse;
import com.nuv.tfgrs.dto.response.StockDistributionRowResponse;
import com.nuv.tfgrs.dto.response.StockPorCategoriaRowResponse;
import com.nuv.tfgrs.dto.response.TopProductoVelocidadRowResponse;
import com.nuv.tfgrs.dto.response.VelocidadResumenRowResponse;
import com.nuv.tfgrs.service.AnaliticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnaliticaServiceImpl implements AnaliticaService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<InventorySummaryRowResponse> inventarioResumen() {
        String sql = """
                SELECT product_id, sku, product_name, category_name, current_quantity, minimum_stock_level,
                       unit_price, stock_value, stock_status
                FROM v_inventory_summary
                ORDER BY stock_value DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new InventorySummaryRowResponse(
                rs.getLong("product_id"),
                rs.getString("sku"),
                rs.getString("product_name"),
                rs.getString("category_name"),
                rs.getInt("current_quantity"),
                rs.getInt("minimum_stock_level"),
                rs.getBigDecimal("unit_price"),
                rs.getBigDecimal("stock_value"),
                rs.getString("stock_status")
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesVelocityRowResponse> velocidadVentas(int limit, Integer days) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        Integer safeDays = normalizeDays(days);
        String sqlBase = """
                SELECT product_id, sku, product_name, category_name, movement_date, units_sold, revenue
                FROM v_sales_velocity
                """;
        String sql = safeDays == null
                ? sqlBase + "\nORDER BY movement_date DESC, revenue DESC\nLIMIT :limit"
                : sqlBase + "\nWHERE movement_date >= :dateFrom\nORDER BY movement_date DESC, revenue DESC\nLIMIT :limit";

        MapSqlParameterSource params = new MapSqlParameterSource("limit", safeLimit);
        if (safeDays != null) {
            params.addValue("dateFrom", LocalDate.now().minusDays(safeDays));
        }

        return jdbcTemplate.query(sql, params, (rs, rowNum) ->
                new SalesVelocityRowResponse(
                        rs.getLong("product_id"),
                        rs.getString("sku"),
                        rs.getString("product_name"),
                        rs.getString("category_name"),
                        rs.getDate("movement_date").toLocalDate(),
                        rs.getInt("units_sold"),
                        rs.getBigDecimal("revenue")
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceTrendRowResponse> tendenciasPrecios(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        String sql = """
                SELECT history_id, product_id, sku, product_name, category_name,
                       old_price, new_price, price_delta, pct_change, changed_at, current_price
                FROM v_price_trends
                ORDER BY changed_at DESC
                LIMIT :limit
                """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource("limit", safeLimit), (rs, rowNum) ->
                new PriceTrendRowResponse(
                        rs.getLong("history_id"),
                        rs.getLong("product_id"),
                        rs.getString("sku"),
                        rs.getString("product_name"),
                        rs.getString("category_name"),
                        rs.getBigDecimal("old_price"),
                        rs.getBigDecimal("new_price"),
                        rs.getBigDecimal("price_delta"),
                        rs.getBigDecimal("pct_change"),
                        rs.getTimestamp("changed_at").toLocalDateTime(),
                        rs.getBigDecimal("current_price")
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BundleCandidateRowResponse> candidatosBundle(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        String sql = """
                SELECT product_a_id, product_a_sku, product_a_name, product_a_price,
                       product_b_id, product_b_sku, product_b_name, product_b_price,
                       co_occurrence_count, combined_price
                FROM v_bundle_candidates
                ORDER BY co_occurrence_count DESC, combined_price DESC
                LIMIT :limit
                """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource("limit", safeLimit), (rs, rowNum) ->
                new BundleCandidateRowResponse(
                        rs.getLong("product_a_id"),
                        rs.getString("product_a_sku"),
                        rs.getString("product_a_name"),
                        rs.getBigDecimal("product_a_price"),
                        rs.getLong("product_b_id"),
                        rs.getString("product_b_sku"),
                        rs.getString("product_b_name"),
                        rs.getBigDecimal("product_b_price"),
                        rs.getLong("co_occurrence_count"),
                        rs.getBigDecimal("combined_price")
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<VelocidadResumenRowResponse> velocidadResumen(int days) {
        Integer safeDays = normalizeDays(days);
        String sqlBase = """
                SELECT movement_date, SUM(units_sold) AS units_sold, SUM(revenue) AS revenue
                FROM v_sales_velocity
                GROUP BY movement_date
                ORDER BY movement_date
                """;
        String sql = safeDays == null
                ? sqlBase
                : """
                SELECT movement_date, SUM(units_sold) AS units_sold, SUM(revenue) AS revenue
                FROM v_sales_velocity
                WHERE movement_date >= :dateFrom
                GROUP BY movement_date
                ORDER BY movement_date
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (safeDays != null) {
            params.addValue("dateFrom", LocalDate.now().minusDays(safeDays));
        }

        return jdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> new VelocidadResumenRowResponse(
                        rs.getDate("movement_date").toLocalDate(),
                        rs.getInt("units_sold"),
                        rs.getBigDecimal("revenue")
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopProductoVelocidadRowResponse> topProductosVelocidad(int days, int limit) {
        Integer safeDays = normalizeDays(days);
        int safeLimit = Math.max(3, Math.min(limit, 20));
        String sqlBase = """
                SELECT product_id, sku, product_name, category_name,
                       SUM(units_sold) AS units_sold,
                       SUM(revenue) AS revenue
                FROM v_sales_velocity
                GROUP BY product_id, sku, product_name, category_name
                """;
        String sql = safeDays == null
                ? sqlBase + "\nORDER BY units_sold DESC, revenue DESC\nLIMIT :limit"
                : """
                SELECT product_id, sku, product_name, category_name,
                       SUM(units_sold) AS units_sold,
                       SUM(revenue) AS revenue
                FROM v_sales_velocity
                WHERE movement_date >= :dateFrom
                GROUP BY product_id, sku, product_name, category_name
                ORDER BY units_sold DESC, revenue DESC
                LIMIT :limit
                """;

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("limit", safeLimit);
        if (safeDays != null) {
            params.addValue("dateFrom", LocalDate.now().minusDays(safeDays));
        }

        return jdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> new TopProductoVelocidadRowResponse(
                        rs.getLong("product_id"),
                        rs.getString("sku"),
                        rs.getString("product_name"),
                        rs.getString("category_name"),
                        rs.getInt("units_sold"),
                        rs.getBigDecimal("revenue")
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockDistributionRowResponse> distribucionStock() {
        String sql = """
                SELECT stock_status,
                       COUNT(*) AS products,
                       COALESCE(SUM(current_quantity), 0) AS quantity_total,
                       COALESCE(SUM(stock_value), 0) AS stock_value_total
                FROM v_inventory_summary
                GROUP BY stock_status
                ORDER BY FIELD(stock_status, 'OUT_OF_STOCK', 'LOW_STOCK', 'OK', 'OVERSTOCK')
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new StockDistributionRowResponse(
                rs.getString("stock_status"),
                rs.getLong("products"),
                rs.getLong("quantity_total"),
                rs.getBigDecimal("stock_value_total")
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockPorCategoriaRowResponse> stockPorCategoria() {
        String sql = """
                SELECT c.id AS category_id,
                       c.name AS category_name,
                       COUNT(*) AS products,
                       COALESCE(SUM(p.current_quantity), 0) AS quantity_total,
                       COALESCE(SUM(ROUND(p.current_quantity * p.unit_price, 2)), 0) AS stock_value_total
                FROM products p
                JOIN categories c ON c.id = p.category_id
                GROUP BY c.id, c.name
                ORDER BY stock_value_total DESC, category_name
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new StockPorCategoriaRowResponse(
                rs.getLong("category_id"),
                rs.getString("category_name"),
                rs.getLong("products"),
                rs.getLong("quantity_total"),
                rs.getBigDecimal("stock_value_total")
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public FinanzasResumenResponse finanzasResumen(int days) {
        Integer safeDays = normalizeDays(days);
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (safeDays != null) {
            params.addValue("dateFrom", LocalDate.now().minusDays(safeDays));
        }

        String ventasSqlBase = """
                SELECT
                    COALESCE(SUM(s.total_amount), 0) AS ventas,
                    COALESCE(COUNT(*), 0) AS ventas_count
                FROM sales s
                """;
        String ventasSql = safeDays == null ? ventasSqlBase : ventasSqlBase + "\nWHERE s.sold_at >= :dateFrom";
        Map<String, Object> ventasRow = jdbcTemplate.queryForMap(ventasSql, params);

        String comprasSql = """
                SELECT
                    COALESCE(SUM(poi.quantity * poi.unit_price_snapshot), 0) AS compras,
                    COALESCE(COUNT(DISTINCT po.id), 0) AS ordenes_recibidas_count
                FROM purchase_orders po
                JOIN purchase_order_items poi ON poi.order_id = po.id
                WHERE po.status = 'RECEIVED'
                  AND po.received_at IS NOT NULL
                """;
        if (safeDays != null) {
            comprasSql = comprasSql + "\n  AND po.received_at >= :dateFrom";
        }
        Map<String, Object> comprasRow = jdbcTemplate.queryForMap(comprasSql, params);

        String stockVentaSql = """
                SELECT COALESCE(SUM(ROUND(p.current_quantity * p.unit_price, 2)), 0) AS valor_stock_venta
                FROM products p
                """;
        Map<String, Object> stockVentaRow = jdbcTemplate.queryForMap(stockVentaSql, new MapSqlParameterSource());

        String costoStockSql = """
                SELECT COALESCE(SUM(
                    p.current_quantity * COALESCE(
                        (
                            SELECT poi.unit_price_snapshot
                            FROM purchase_order_items poi
                            JOIN purchase_orders po ON po.id = poi.order_id
                            WHERE poi.product_id = p.id
                              AND po.status = 'RECEIVED'
                              AND po.received_at IS NOT NULL
                            ORDER BY po.received_at DESC, po.id DESC
                            LIMIT 1
                        ),
                        COALESCE(p.acquisition_price, p.unit_price)
                    )
                ), 0) AS costo_stock
                FROM products p
                """;
        Map<String, Object> costoStockRow = jdbcTemplate.queryForMap(costoStockSql, new MapSqlParameterSource());

        String cogsRealSql = """
                SELECT COALESCE(SUM(si.quantity * si.cost_price_snapshot), 0) AS cogs_real
                FROM sale_items si
                JOIN sales s ON s.id = si.sale_id
                """;
        if (safeDays != null) {
            cogsRealSql = cogsRealSql + "\nWHERE s.sold_at >= :dateFrom";
        }
        Map<String, Object> cogsRow = jdbcTemplate.queryForMap(cogsRealSql, params);

        var ventas = asBigDecimal(ventasRow.get("ventas"));
        var compras = asBigDecimal(comprasRow.get("compras"));
        var flujoNeto = ventas.subtract(compras);
        var valorStockVenta = asBigDecimal(stockVentaRow.get("valor_stock_venta"));
        var costoStock = asBigDecimal(costoStockRow.get("costo_stock"));
        var diferenciaStock = valorStockVenta.subtract(costoStock);
        var cogsReal = asBigDecimal(cogsRow.get("cogs_real"));
        var margenReal = ventas.subtract(cogsReal);

        return new FinanzasResumenResponse(
                safeDays == null ? 0 : safeDays,
                ventas,
                compras,
                flujoNeto,
                cogsReal,
                margenReal,
                valorStockVenta,
                costoStock,
                diferenciaStock,
                asLong(ventasRow.get("ventas_count")),
                asLong(comprasRow.get("ordenes_recibidas_count"))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentabilidadProductoRowResponse> rentabilidadProductos(int days, int limit) {
        Integer safeDays = normalizeDays(days);
        int safeLimit = Math.max(5, Math.min(limit, 50));

        String sqlBase = """
                SELECT
                    p.id AS product_id,
                    p.sku,
                    p.name AS product_name,
                    c.name AS category_name,
                    SUM(si.quantity) AS unidades_vendidas,
                    ROUND(SUM(si.quantity * si.unit_price_snapshot), 2) AS revenue,
                    ROUND(SUM(si.quantity * si.cost_price_snapshot), 2) AS cogs,
                    ROUND(SUM(si.quantity * (si.unit_price_snapshot - si.cost_price_snapshot)), 2) AS margen,
                    CASE
                        WHEN SUM(si.quantity * si.unit_price_snapshot) = 0 THEN 0
                        ELSE ROUND(
                            (
                                SUM(si.quantity * (si.unit_price_snapshot - si.cost_price_snapshot))
                                / SUM(si.quantity * si.unit_price_snapshot)
                            ) * 100,
                            2
                        )
                    END AS margen_pct
                FROM sale_items si
                JOIN sales s ON s.id = si.sale_id
                JOIN products p ON p.id = si.product_id
                JOIN categories c ON c.id = p.category_id
                GROUP BY p.id, p.sku, p.name, c.name
                """;
        String sql = safeDays == null
                ? sqlBase + "\nORDER BY margen DESC, revenue DESC\nLIMIT :limit"
                : """
                SELECT
                    p.id AS product_id,
                    p.sku,
                    p.name AS product_name,
                    c.name AS category_name,
                    SUM(si.quantity) AS unidades_vendidas,
                    ROUND(SUM(si.quantity * si.unit_price_snapshot), 2) AS revenue,
                    ROUND(SUM(si.quantity * si.cost_price_snapshot), 2) AS cogs,
                    ROUND(SUM(si.quantity * (si.unit_price_snapshot - si.cost_price_snapshot)), 2) AS margen,
                    CASE
                        WHEN SUM(si.quantity * si.unit_price_snapshot) = 0 THEN 0
                        ELSE ROUND(
                            (
                                SUM(si.quantity * (si.unit_price_snapshot - si.cost_price_snapshot))
                                / SUM(si.quantity * si.unit_price_snapshot)
                            ) * 100,
                            2
                        )
                    END AS margen_pct
                FROM sale_items si
                JOIN sales s ON s.id = si.sale_id
                JOIN products p ON p.id = si.product_id
                JOIN categories c ON c.id = p.category_id
                WHERE s.sold_at >= :dateFrom
                GROUP BY p.id, p.sku, p.name, c.name
                ORDER BY margen DESC, revenue DESC
                LIMIT :limit
                """;

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("limit", safeLimit);
        if (safeDays != null) {
            params.addValue("dateFrom", LocalDate.now().minusDays(safeDays));
        }

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new RentabilidadProductoRowResponse(
                rs.getLong("product_id"),
                rs.getString("sku"),
                rs.getString("product_name"),
                rs.getString("category_name"),
                rs.getInt("unidades_vendidas"),
                rs.getBigDecimal("revenue"),
                rs.getBigDecimal("cogs"),
                rs.getBigDecimal("margen"),
                rs.getBigDecimal("margen_pct")
        ));
    }

    private java.math.BigDecimal asBigDecimal(Object value) {
        if (value == null) return java.math.BigDecimal.ZERO;
        if (value instanceof java.math.BigDecimal decimal) return decimal;
        if (value instanceof Number number) return java.math.BigDecimal.valueOf(number.doubleValue());
        return java.math.BigDecimal.ZERO;
    }

    private long asLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number number) return number.longValue();
        return 0L;
    }

    private Integer normalizeDays(Integer days) {
        if (days == null) return 30;
        if (days <= 0) return null;
        return Math.min(days, 3650);
    }
}
