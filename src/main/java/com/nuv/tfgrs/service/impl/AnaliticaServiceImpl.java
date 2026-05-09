package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.dto.response.BundleCandidateRowResponse;
import com.nuv.tfgrs.dto.response.FinanzasResumenResponse;
import com.nuv.tfgrs.dto.response.InventorySummaryRowResponse;
import com.nuv.tfgrs.dto.response.AnaliticaMetaResponse;
import com.nuv.tfgrs.dto.response.PriceTrendRowResponse;
import com.nuv.tfgrs.dto.response.RentabilidadProductoRowResponse;
import com.nuv.tfgrs.dto.response.SalesVelocityRowResponse;
import com.nuv.tfgrs.dto.response.StockDistributionRowResponse;
import com.nuv.tfgrs.dto.response.StockPorCategoriaRowResponse;
import com.nuv.tfgrs.dto.response.TopProductoVelocidadRowResponse;
import com.nuv.tfgrs.dto.response.VelocidadResumenRowResponse;
import com.nuv.tfgrs.security.org.CurrentOrganizationResolver;
import com.nuv.tfgrs.service.AnaliticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AnaliticaServiceImpl implements AnaliticaService {

    private static final String ANALYTICS_SEED_KEY = "analytics_seed_v3";
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CurrentOrganizationResolver currentOrganizationResolver;
    @Value("${analytics.seed.enabled:false}")
    private boolean analyticsSeedEnabled;

    @Override
    @Transactional(readOnly = true)
    public List<InventorySummaryRowResponse> inventarioResumen() {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        String sql = """
                SELECT v.product_id, v.sku, v.product_name, v.category_name, v.current_quantity, v.minimum_stock_level,
                       v.unit_price, v.stock_value, v.stock_status
                FROM v_inventory_summary v
                JOIN products p ON p.id = v.product_id
                WHERE p.organization_id = :organizationId
                ORDER BY v.stock_value DESC
                """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource("organizationId", organizationId), (rs, rowNum) -> new InventorySummaryRowResponse(
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
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        int safeLimit = Math.max(1, Math.min(limit, 200));
        Integer safeDays = normalizeDays(days);
        String sqlBase = """
                SELECT v.product_id, v.sku, v.product_name, v.category_name, v.movement_date, v.units_sold, v.revenue
                FROM v_sales_velocity v
                JOIN products p ON p.id = v.product_id
                WHERE p.organization_id = :organizationId
                """;
        String sql = safeDays == null
                ? sqlBase + "\nORDER BY v.movement_date DESC, v.revenue DESC\nLIMIT :limit"
                : sqlBase + "\nAND v.movement_date >= :dateFrom\nORDER BY v.movement_date DESC, v.revenue DESC\nLIMIT :limit";

        MapSqlParameterSource params = new MapSqlParameterSource("limit", safeLimit)
                .addValue("organizationId", organizationId);
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
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        int safeLimit = Math.max(1, Math.min(limit, 200));
        String sql = """
                SELECT v.history_id, v.product_id, v.sku, v.product_name, v.category_name,
                       v.old_price, v.new_price, v.price_delta, v.pct_change, v.changed_at, v.current_price
                FROM v_price_trends v
                JOIN products p ON p.id = v.product_id
                WHERE p.organization_id = :organizationId
                ORDER BY v.changed_at DESC
                LIMIT :limit
                """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource("limit", safeLimit)
                .addValue("organizationId", organizationId), (rs, rowNum) ->
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
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        int safeLimit = Math.max(1, Math.min(limit, 200));
        String sql = """
                SELECT v.product_a_id, v.product_a_sku, v.product_a_name, v.product_a_price,
                       v.product_b_id, v.product_b_sku, v.product_b_name, v.product_b_price,
                       v.co_occurrence_count, v.combined_price
                FROM v_bundle_candidates v
                JOIN products p ON p.id = v.product_a_id
                WHERE p.organization_id = :organizationId
                ORDER BY v.co_occurrence_count DESC, v.combined_price DESC
                LIMIT :limit
                """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource("limit", safeLimit)
                .addValue("organizationId", organizationId), (rs, rowNum) ->
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
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        Integer safeDays = normalizeDays(days);
        String sqlBase = """
                SELECT v.movement_date, SUM(v.units_sold) AS units_sold, SUM(v.revenue) AS revenue
                FROM v_sales_velocity v
                JOIN products p ON p.id = v.product_id
                WHERE p.organization_id = :organizationId
                GROUP BY v.movement_date
                ORDER BY v.movement_date
                """;
        String sql = safeDays == null
                ? sqlBase
                : """
                SELECT v.movement_date, SUM(v.units_sold) AS units_sold, SUM(v.revenue) AS revenue
                FROM v_sales_velocity v
                JOIN products p ON p.id = v.product_id
                WHERE p.organization_id = :organizationId
                  AND v.movement_date >= :dateFrom
                GROUP BY v.movement_date
                ORDER BY v.movement_date
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("organizationId", organizationId);
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
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        Integer safeDays = normalizeDays(days);
        int safeLimit = Math.max(3, Math.min(limit, 20));
        String sqlBase = """
                SELECT v.product_id, v.sku, v.product_name, v.category_name,
                       SUM(v.units_sold) AS units_sold,
                       SUM(v.revenue) AS revenue
                FROM v_sales_velocity v
                JOIN products p ON p.id = v.product_id
                WHERE p.organization_id = :organizationId
                GROUP BY v.product_id, v.sku, v.product_name, v.category_name
                """;
        String sql = safeDays == null
                ? sqlBase + "\nORDER BY units_sold DESC, revenue DESC\nLIMIT :limit"
                : """
                SELECT v.product_id, v.sku, v.product_name, v.category_name,
                       SUM(v.units_sold) AS units_sold,
                       SUM(v.revenue) AS revenue
                FROM v_sales_velocity v
                JOIN products p ON p.id = v.product_id
                WHERE p.organization_id = :organizationId
                  AND v.movement_date >= :dateFrom
                GROUP BY v.product_id, v.sku, v.product_name, v.category_name
                ORDER BY units_sold DESC, revenue DESC
                LIMIT :limit
                """;

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("limit", safeLimit)
                .addValue("organizationId", organizationId);
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
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        String sql = """
                SELECT v.stock_status,
                       COUNT(*) AS products,
                       COALESCE(SUM(v.current_quantity), 0) AS quantity_total,
                       COALESCE(SUM(v.stock_value), 0) AS stock_value_total
                FROM v_inventory_summary v
                JOIN products p ON p.id = v.product_id
                WHERE p.organization_id = :organizationId
                GROUP BY v.stock_status
                ORDER BY FIELD(v.stock_status, 'OUT_OF_STOCK', 'LOW_STOCK', 'OK', 'OVERSTOCK')
                """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource("organizationId", organizationId), (rs, rowNum) -> new StockDistributionRowResponse(
                rs.getString("stock_status"),
                rs.getLong("products"),
                rs.getLong("quantity_total"),
                rs.getBigDecimal("stock_value_total")
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockPorCategoriaRowResponse> stockPorCategoria() {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        String sql = """
                SELECT c.id AS category_id,
                       c.name AS category_name,
                       COUNT(*) AS products,
                       COALESCE(SUM(p.current_quantity), 0) AS quantity_total,
                       COALESCE(SUM(ROUND(p.current_quantity * p.unit_price, 2)), 0) AS stock_value_total
                FROM products p
                JOIN categories c ON c.id = p.category_id
                WHERE p.organization_id = :organizationId
                GROUP BY c.id, c.name
                ORDER BY stock_value_total DESC, category_name
                """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource("organizationId", organizationId), (rs, rowNum) -> new StockPorCategoriaRowResponse(
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
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        Integer safeDays = normalizeDays(days);
        MapSqlParameterSource params = new MapSqlParameterSource("organizationId", organizationId);
        if (safeDays != null) {
            params.addValue("dateFrom", LocalDate.now().minusDays(safeDays));
        }

        String ventasSqlBase = """
                SELECT
                    COALESCE(SUM(s.total_amount), 0) AS ventas,
                    COALESCE(COUNT(*), 0) AS ventas_count
                FROM sales s
                WHERE s.organization_id = :organizationId
                """;
        String ventasSql = safeDays == null ? ventasSqlBase : ventasSqlBase + "\nAND s.sold_at >= :dateFrom";
        Map<String, Object> ventasRow = jdbcTemplate.queryForMap(ventasSql, params);

        String comprasSql = """
                SELECT
                    COALESCE(SUM(poi.quantity * poi.unit_price_snapshot), 0) AS compras,
                    COALESCE(COUNT(DISTINCT po.id), 0) AS ordenes_recibidas_count
                FROM purchase_orders po
                JOIN purchase_order_items poi ON poi.order_id = po.id
                WHERE po.status = 'RECEIVED'
                  AND po.received_at IS NOT NULL
                  AND po.organization_id = :organizationId
                """;
        if (safeDays != null) {
            comprasSql = comprasSql + "\n  AND po.received_at >= :dateFrom";
        }
        Map<String, Object> comprasRow = jdbcTemplate.queryForMap(comprasSql, params);

        String stockVentaSql = """
                SELECT COALESCE(SUM(ROUND(p.current_quantity * p.unit_price, 2)), 0) AS valor_stock_venta
                FROM products p
                WHERE p.organization_id = :organizationId
                """;
        Map<String, Object> stockVentaRow = jdbcTemplate.queryForMap(stockVentaSql, new MapSqlParameterSource("organizationId", organizationId));

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
                              AND po.organization_id = :organizationId
                            ORDER BY po.received_at DESC, po.id DESC
                            LIMIT 1
                        ),
                        COALESCE(p.acquisition_price, p.unit_price)
                    )
                ), 0) AS costo_stock
                FROM products p
                WHERE p.organization_id = :organizationId
                """;
        Map<String, Object> costoStockRow = jdbcTemplate.queryForMap(costoStockSql, new MapSqlParameterSource("organizationId", organizationId));

        String cogsRealSql = """
                SELECT COALESCE(SUM(si.quantity * si.cost_price_snapshot), 0) AS cogs_real
                FROM sale_items si
                JOIN sales s ON s.id = si.sale_id
                WHERE s.organization_id = :organizationId
                """;
        if (safeDays != null) {
            cogsRealSql = cogsRealSql + "\nAND s.sold_at >= :dateFrom";
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

        long ventasCount = asLong(ventasRow.get("ventas_count"));
        int periodDays = resolvePeriodDays(safeDays, params);
        BigDecimal ticketPromedio = safeDivide(ventas, BigDecimal.valueOf(ventasCount), 2);
        BigDecimal margenBrutoPct = safePercent(margenReal, ventas, 2);
        BigDecimal markupPct = safePercent(margenReal, cogsReal, 2);
        BigDecimal ventasPromedioDiarias = safeDivide(ventas, BigDecimal.valueOf(periodDays), 2);

        BigDecimal inventarioInicialCosto = costoStock.subtract(compras).add(cogsReal);
        if (inventarioInicialCosto.compareTo(BigDecimal.ZERO) < 0) {
            inventarioInicialCosto = BigDecimal.ZERO;
        }
        BigDecimal inventarioPromedioCosto = inventarioInicialCosto
                .add(costoStock)
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal rotacionInventario = safeDivide(cogsReal, inventarioPromedioCosto, 4);
        BigDecimal diasInventario = rotacionInventario.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(periodDays).divide(rotacionInventario, 2, RoundingMode.HALF_UP);
        BigDecimal gmroi = safeDivide(margenReal, inventarioPromedioCosto, 4);

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
                ventasCount,
                asLong(comprasRow.get("ordenes_recibidas_count")),
                margenBrutoPct,
                markupPct,
                ticketPromedio,
                inventarioPromedioCosto,
                rotacionInventario,
                diasInventario,
                gmroi,
                ventasPromedioDiarias
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentabilidadProductoRowResponse> rentabilidadProductos(int days, int limit) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
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
                WHERE s.organization_id = :organizationId
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
                WHERE s.organization_id = :organizationId
                  AND s.sold_at >= :dateFrom
                GROUP BY p.id, p.sku, p.name, c.name
                ORDER BY margen DESC, revenue DESC
                LIMIT :limit
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", safeLimit)
                .addValue("organizationId", organizationId);
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

    @Override
    @Transactional(readOnly = true)
    public AnaliticaMetaResponse metadata(int days) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        Integer safeDays = normalizeDays(days);
        int resolvedWindowDays = safeDays == null ? 0 : safeDays;
        MapSqlParameterSource params = new MapSqlParameterSource("organizationId", organizationId);
        if (safeDays != null) {
            params.addValue("dateFrom", LocalDate.now().minusDays(safeDays));
        }

        String salesSql = safeDays == null
                ? "SELECT MAX(sold_at) AS latest FROM sales WHERE organization_id = :organizationId"
                : "SELECT MAX(sold_at) AS latest FROM sales WHERE organization_id = :organizationId AND sold_at >= :dateFrom";
        String purchaseSql = safeDays == null
                ? "SELECT MAX(received_at) AS latest FROM purchase_orders WHERE organization_id = :organizationId AND status = 'RECEIVED'"
                : "SELECT MAX(received_at) AS latest FROM purchase_orders WHERE organization_id = :organizationId AND status = 'RECEIVED' AND received_at >= :dateFrom";
        String pricesSql = safeDays == null
                ? "SELECT MAX(changed_at) AS latest FROM price_history WHERE organization_id = :organizationId"
                : "SELECT MAX(changed_at) AS latest FROM price_history WHERE organization_id = :organizationId AND changed_at >= :dateFrom";

        LocalDateTime latestSales = asLocalDateTime(jdbcTemplate.queryForMap(salesSql, params).get("latest"));
        LocalDateTime latestPurchases = asLocalDateTime(jdbcTemplate.queryForMap(purchaseSql, params).get("latest"));
        LocalDateTime latestPriceChange = asLocalDateTime(jdbcTemplate.queryForMap(pricesSql, params).get("latest"));

        LocalDateTime generatedAt = Stream.of(latestSales, latestPurchases, latestPriceChange)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        boolean seedApplied = isAnalyticsSeedApplied();
        String source = analyticsSeedEnabled
                ? (seedApplied ? "API_REAL_LOCAL_SEEDED" : "API_REAL_SEED_PENDING")
                : "API_REAL";

        return new AnaliticaMetaResponse(source, analyticsSeedEnabled, generatedAt, resolvedWindowDays);
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

    private LocalDateTime asLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime localDateTime) return localDateTime;
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime();
        if (value instanceof java.sql.Date date) return date.toLocalDate().atStartOfDay();
        return null;
    }

    private boolean isAnalyticsSeedApplied() {
        try {
            Integer tableExists = jdbcTemplate.queryForObject("""
                            SELECT COUNT(*)
                            FROM information_schema.tables
                            WHERE table_schema = DATABASE()
                              AND table_name = 'analytics_seed_meta'
                            """,
                    new MapSqlParameterSource(),
                    Integer.class
            );

            if (tableExists == null || tableExists == 0) {
                return false;
            }

            Integer seedKeyCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM analytics_seed_meta WHERE seed_key = :seedKey",
                    new MapSqlParameterSource("seedKey", ANALYTICS_SEED_KEY),
                    Integer.class
            );

            return seedKeyCount != null && seedKeyCount > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private int resolvePeriodDays(Integer safeDays, MapSqlParameterSource params) {
        if (safeDays != null) {
            return Math.max(safeDays, 1);
        }

        String sql = """
                SELECT COALESCE(DATEDIFF(MAX(s.sold_at), MIN(s.sold_at)) + 1, 0) AS period_days
                FROM sales s
                WHERE s.organization_id = :organizationId
                """;
        Long dbDays = asLong(jdbcTemplate.queryForMap(sql, params).get("period_days"));
        if (dbDays <= 0) {
            return 1;
        }
        return dbDays > Integer.MAX_VALUE ? Integer.MAX_VALUE : dbDays.intValue();
    }

    private BigDecimal safeDivide(BigDecimal numerator, BigDecimal denominator, int scale) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        }
        if (numerator == null) {
            return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        }
        return numerator.divide(denominator, scale, RoundingMode.HALF_UP);
    }

    private BigDecimal safePercent(BigDecimal numerator, BigDecimal denominator, int scale) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        }
        if (numerator == null) {
            return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        }
        return numerator
                .multiply(BigDecimal.valueOf(100))
                .divide(denominator, scale, RoundingMode.HALF_UP);
    }

    private Integer normalizeDays(Integer days) {
        if (days == null) return 30;
        if (days <= 0) return null;
        return Math.min(days, 3650);
    }
}
