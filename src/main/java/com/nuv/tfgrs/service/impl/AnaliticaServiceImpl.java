package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.dto.response.BundleCandidateRowResponse;
import com.nuv.tfgrs.dto.response.BundlePerformanceRowResponse;
import com.nuv.tfgrs.dto.response.ClienteRentabilidadRowResponse;
import com.nuv.tfgrs.dto.response.FinanzasResumenResponse;
import com.nuv.tfgrs.dto.response.InventorySummaryRowResponse;
import com.nuv.tfgrs.dto.response.AlertaSerieRowResponse;
import com.nuv.tfgrs.dto.response.AnaliticaMetaResponse;
import com.nuv.tfgrs.dto.response.PriceTrendRowResponse;
import com.nuv.tfgrs.dto.response.ProductCreationRowResponse;
import com.nuv.tfgrs.dto.response.ProveedorPerformanceRowResponse;
import com.nuv.tfgrs.dto.response.RentabilidadProductoRowResponse;
import com.nuv.tfgrs.dto.response.RevenueComparativaBucketResponse;
import com.nuv.tfgrs.dto.response.RevenueComparativaResponse;
import com.nuv.tfgrs.dto.response.SalesVelocityRowResponse;
import com.nuv.tfgrs.dto.response.StockDistributionRowResponse;
import com.nuv.tfgrs.dto.response.StockPorCategoriaRowResponse;
import com.nuv.tfgrs.dto.response.StockReceiptRowResponse;
import com.nuv.tfgrs.dto.response.TopProductoVelocidadRowResponse;
import com.nuv.tfgrs.dto.response.VelocidadResumenRowResponse;
import com.nuv.tfgrs.security.org.CurrentOrganizationResolver;
import com.nuv.tfgrs.security.user.CurrentUserResolver;
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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AnaliticaServiceImpl implements AnaliticaService {

    private static final String ANALYTICS_SEED_KEY = "analytics_seed_v3";
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CurrentOrganizationResolver currentOrganizationResolver;
    private final CurrentUserResolver currentUserResolver;
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
        ZoneId zone = resolveUserZone();
        String userTimeZone = zone.getId();
        int safeLimit = Math.max(1, Math.min(limit, 200));
        Integer safeDays = normalizeDays(days);
        String sqlBase = """
                SELECT
                    p.id AS product_id,
                    p.sku,
                    p.name AS product_name,
                    c.name AS category_name,
                    DATE(CONVERT_TZ(s.sold_at, '+00:00', :userTz)) AS movement_date,
                    SUM(si.quantity) AS units_sold,
                    ROUND(SUM(si.quantity * si.unit_price_snapshot), 2) AS revenue
                FROM sales s
                JOIN sale_items si ON si.sale_id = s.id
                JOIN products p ON p.id = si.product_id
                JOIN categories c ON c.id = p.category_id
                WHERE s.organization_id = :organizationId
                """;
        String sql = safeDays == null
                ? sqlBase + "\nGROUP BY p.id, p.sku, p.name, c.name, DATE(CONVERT_TZ(s.sold_at, '+00:00', :userTz))\nORDER BY movement_date DESC, revenue DESC\nLIMIT :limit"
                : sqlBase + "\nAND s.sold_at >= :dateFrom\nGROUP BY p.id, p.sku, p.name, c.name, DATE(CONVERT_TZ(s.sold_at, '+00:00', :userTz))\nORDER BY movement_date DESC, revenue DESC\nLIMIT :limit";

        MapSqlParameterSource params = new MapSqlParameterSource("limit", safeLimit)
                .addValue("organizationId", organizationId)
                .addValue("userTz", userTimeZone);
        if (safeDays != null) {
            params.addValue("dateFrom", toUtcStartOfDayTimestamp(zone, safeDays));
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
    public List<PriceTrendRowResponse> tendenciasPrecios(int limit, Integer days) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        ZoneId zone = resolveUserZone();
        int safeLimit = Math.max(1, Math.min(limit, 200));
        Integer safeDays = normalizeDays(days);
        String sqlBase = """
                SELECT v.history_id, v.product_id, v.sku, v.product_name, v.category_name,
                       v.old_price, v.new_price, v.price_delta, v.pct_change, v.changed_at, v.current_price
                FROM v_price_trends v
                JOIN products p ON p.id = v.product_id
                WHERE p.organization_id = :organizationId
                """;
        String sql = safeDays == null
                ? sqlBase + "\nORDER BY v.changed_at DESC\nLIMIT :limit"
                : sqlBase + "\nAND v.changed_at >= :dateFrom\nORDER BY v.changed_at DESC\nLIMIT :limit";

        MapSqlParameterSource params = new MapSqlParameterSource("limit", safeLimit)
                .addValue("organizationId", organizationId);
        if (safeDays != null) {
            params.addValue("dateFrom", toUtcStartOfDayTimestamp(zone, safeDays));
        }

        return jdbcTemplate.query(sql, params, (rs, rowNum) ->
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
    public List<BundleCandidateRowResponse> candidatosBundle(int limit, Integer days) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        ZoneId zone = resolveUserZone();
        int safeLimit = Math.max(1, Math.min(limit, 200));
        Integer safeDays = normalizeDays(days);
        String sqlBase = """
                SELECT
                    a.product_id AS product_a_id,
                    pa.sku AS product_a_sku,
                    pa.name AS product_a_name,
                    pa.unit_price AS product_a_price,
                    b.product_id AS product_b_id,
                    pb.sku AS product_b_sku,
                    pb.name AS product_b_name,
                    pb.unit_price AS product_b_price,
                    COUNT(DISTINCT a.sale_id) AS co_occurrence_count,
                    ROUND(pa.unit_price + pb.unit_price, 2) AS combined_price
                FROM sale_items a
                JOIN sale_items b
                    ON a.sale_id = b.sale_id
                   AND a.product_id < b.product_id
                JOIN sales s ON s.id = a.sale_id
                JOIN products pa ON pa.id = a.product_id
                JOIN products pb ON pb.id = b.product_id
                WHERE s.organization_id = :organizationId
                """;
        String sql = safeDays == null
                ? sqlBase + """
                GROUP BY
                    a.product_id, pa.sku, pa.name, pa.unit_price,
                    b.product_id, pb.sku, pb.name, pb.unit_price
                HAVING COUNT(DISTINCT a.sale_id) >= 2
                ORDER BY co_occurrence_count DESC, combined_price DESC
                LIMIT :limit
                """
                : sqlBase + """
                  AND s.sold_at >= :dateFrom
                GROUP BY
                    a.product_id, pa.sku, pa.name, pa.unit_price,
                    b.product_id, pb.sku, pb.name, pb.unit_price
                HAVING COUNT(DISTINCT a.sale_id) >= 2
                ORDER BY co_occurrence_count DESC, combined_price DESC
                LIMIT :limit
                """;

        MapSqlParameterSource params = new MapSqlParameterSource("limit", safeLimit)
                .addValue("organizationId", organizationId);
        if (safeDays != null) {
            params.addValue("dateFrom", toUtcStartOfDayTimestamp(zone, safeDays));
        }

        return jdbcTemplate.query(sql, params, (rs, rowNum) ->
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
        ZoneId zone = resolveUserZone();
        String userTimeZone = zone.getId();
        Integer safeDays = normalizeDays(days);
        String sqlBase = """
                SELECT
                    DATE(CONVERT_TZ(s.sold_at, '+00:00', :userTz)) AS movement_date,
                    SUM(si.quantity) AS units_sold,
                    ROUND(SUM(si.quantity * si.unit_price_snapshot), 2) AS revenue
                FROM sales s
                JOIN sale_items si ON si.sale_id = s.id
                WHERE s.organization_id = :organizationId
                GROUP BY DATE(CONVERT_TZ(s.sold_at, '+00:00', :userTz))
                ORDER BY movement_date
                """;
        String sql = safeDays == null
                ? sqlBase
                : """
                SELECT
                    DATE(CONVERT_TZ(s.sold_at, '+00:00', :userTz)) AS movement_date,
                    SUM(si.quantity) AS units_sold,
                    ROUND(SUM(si.quantity * si.unit_price_snapshot), 2) AS revenue
                FROM sales s
                JOIN sale_items si ON si.sale_id = s.id
                WHERE s.organization_id = :organizationId
                  AND s.sold_at >= :dateFrom
                GROUP BY DATE(CONVERT_TZ(s.sold_at, '+00:00', :userTz))
                ORDER BY movement_date
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("organizationId", organizationId)
                .addValue("userTz", userTimeZone);
        if (safeDays != null) {
            params.addValue("dateFrom", toUtcStartOfDayTimestamp(zone, safeDays));
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
        ZoneId zone = resolveUserZone();
        Integer safeDays = normalizeDays(days);
        int safeLimit = Math.max(3, Math.min(limit, 20));
        String sqlBase = """
                SELECT p.id AS product_id, p.sku, p.name AS product_name, c.name AS category_name,
                       SUM(si.quantity) AS units_sold,
                       ROUND(SUM(si.quantity * si.unit_price_snapshot), 2) AS revenue
                FROM sales s
                JOIN sale_items si ON si.sale_id = s.id
                JOIN products p ON p.id = si.product_id
                JOIN categories c ON c.id = p.category_id
                WHERE s.organization_id = :organizationId
                GROUP BY p.id, p.sku, p.name, c.name
                """;
        String sql = safeDays == null
                ? sqlBase + "\nORDER BY units_sold DESC, revenue DESC\nLIMIT :limit"
                : """
                SELECT p.id AS product_id, p.sku, p.name AS product_name, c.name AS category_name,
                       SUM(si.quantity) AS units_sold,
                       ROUND(SUM(si.quantity * si.unit_price_snapshot), 2) AS revenue
                FROM sales s
                JOIN sale_items si ON si.sale_id = s.id
                JOIN products p ON p.id = si.product_id
                JOIN categories c ON c.id = p.category_id
                WHERE s.organization_id = :organizationId
                  AND s.sold_at >= :dateFrom
                GROUP BY p.id, p.sku, p.name, c.name
                ORDER BY units_sold DESC, revenue DESC
                LIMIT :limit
                """;

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("limit", safeLimit)
                .addValue("organizationId", organizationId);
        if (safeDays != null) {
            params.addValue("dateFrom", toUtcStartOfDayTimestamp(zone, safeDays));
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
        ZoneId zone = resolveUserZone();
        Integer safeDays = normalizeDays(days);
        MapSqlParameterSource params = new MapSqlParameterSource("organizationId", organizationId);
        if (safeDays != null) {
            params.addValue("dateFrom", toUtcStartOfDayTimestamp(zone, safeDays));
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
    public RevenueComparativaResponse revenueComparativa(int days) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        ZoneId zone = resolveUserZone();
        LocalDateTime now = LocalDateTime.now(zone);
        RevenueTimelineSpec spec = resolveRevenueTimelineSpec(organizationId, days, now, zone);
        if (spec.bucketCount() <= 0) {
            return new RevenueComparativaResponse(spec.currentLabel(), spec.previousLabel(), spec.granularity().name(), List.of());
        }

        LocalDateTime previousStart = spec.currentStart().minusYears(1);
        LocalDateTime previousEnd = spec.currentEnd().minusYears(1);
        LocalDateTime queryFrom = previousStart.isBefore(spec.currentStart()) ? previousStart : spec.currentStart();

        String sql = """
                SELECT s.sold_at, s.total_amount
                FROM sales s
                WHERE s.organization_id = :organizationId
                  AND s.sold_at >= :fromDate
                  AND s.sold_at <= :toDate
                ORDER BY s.sold_at
                """;
        List<SaleRevenuePoint> points = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("organizationId", organizationId)
                        .addValue("fromDate", Timestamp.from(queryFrom.atZone(zone).toInstant()))
                        .addValue("toDate", Timestamp.from(spec.currentEnd().atZone(zone).toInstant())),
                (rs, rowNum) -> new SaleRevenuePoint(
                        rs.getTimestamp("sold_at").toInstant().atZone(zone).toLocalDateTime(),
                        asBigDecimal(rs.getBigDecimal("total_amount"))
                )
        );

        BigDecimal[] currentTotals = new BigDecimal[spec.bucketCount()];
        BigDecimal[] previousTotals = new BigDecimal[spec.bucketCount()];
        for (int i = 0; i < spec.bucketCount(); i++) {
            currentTotals[i] = BigDecimal.ZERO;
            previousTotals[i] = BigDecimal.ZERO;
        }

        for (SaleRevenuePoint point : points) {
            LocalDateTime soldAt = point.soldAt();
            if (!soldAt.isBefore(spec.currentStart()) && !soldAt.isAfter(spec.currentEnd())) {
                int index = resolveBucketIndex(soldAt, spec.currentStart(), spec.granularity(), spec.bucketCount());
                if (index >= 0) {
                    currentTotals[index] = currentTotals[index].add(point.amount());
                }
                continue;
            }

            if (!soldAt.isBefore(previousStart) && !soldAt.isAfter(previousEnd)) {
                LocalDateTime shifted = soldAt.plusYears(1);
                int index = resolveBucketIndex(shifted, spec.currentStart(), spec.granularity(), spec.bucketCount());
                if (index >= 0) {
                    previousTotals[index] = previousTotals[index].add(point.amount());
                }
            }
        }

        List<RevenueComparativaBucketResponse> buckets = new ArrayList<>(spec.bucketCount());
        for (int i = 0; i < spec.bucketCount(); i++) {
            LocalDateTime bucketStart = resolveBucketStart(spec.currentStart(), spec.granularity(), i);
            String label = resolveBucketLabel(bucketStart, spec.granularity(), spec.requestedDays(), i);
            buckets.add(new RevenueComparativaBucketResponse(
                    label,
                    currentTotals[i].setScale(2, RoundingMode.HALF_UP),
                    previousTotals[i].setScale(2, RoundingMode.HALF_UP)
            ));
        }

        return new RevenueComparativaResponse(spec.currentLabel(), spec.previousLabel(), spec.granularity().name(), buckets);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentabilidadProductoRowResponse> rentabilidadProductos(int days, int limit) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        ZoneId zone = resolveUserZone();
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
            params.addValue("dateFrom", toUtcStartOfDayTimestamp(zone, safeDays));
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
    public List<BundlePerformanceRowResponse> bundlesPerformance(int days, int limit) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        ZoneId zone = resolveUserZone();
        Integer safeDays = normalizeDays(days);
        int safeLimit = Math.max(1, Math.min(limit, 100));

        String sqlBase = """
                SELECT
                    si.bundle_id AS bundle_id,
                    b.name AS bundle_name,
                    SUM(si.quantity) AS units_sold,
                    ROUND(SUM(si.quantity * si.unit_price_snapshot), 2) AS revenue,
                    ROUND(SUM(si.quantity * si.cost_price_snapshot), 2) AS cost,
                    ROUND(SUM(si.quantity * (si.unit_price_snapshot - si.cost_price_snapshot)), 2) AS profit,
                    CASE
                        WHEN SUM(si.quantity * si.unit_price_snapshot) = 0 THEN 0
                        ELSE ROUND(
                            (
                                SUM(si.quantity * (si.unit_price_snapshot - si.cost_price_snapshot))
                                / SUM(si.quantity * si.unit_price_snapshot)
                            ) * 100,
                            2
                        )
                    END AS margin,
                    COUNT(DISTINCT s.id) AS sales_count
                FROM sale_items si
                JOIN sales s ON s.id = si.sale_id
                JOIN bundles b
                  ON b.id = si.bundle_id
                 AND b.organization_id = s.organization_id
                WHERE s.organization_id = :organizationId
                  AND si.bundle_id IS NOT NULL
                """;
        String sql = safeDays == null
                ? sqlBase + """
                GROUP BY si.bundle_id, b.name
                ORDER BY revenue DESC, bundle_name
                LIMIT :limit
                """
                : sqlBase + """
                  AND s.sold_at >= :dateFrom
                GROUP BY si.bundle_id, b.name
                ORDER BY revenue DESC, bundle_name
                LIMIT :limit
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("organizationId", organizationId)
                .addValue("limit", safeLimit);
        if (safeDays != null) {
            params.addValue("dateFrom", toUtcStartOfDayTimestamp(zone, safeDays));
        }

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new BundlePerformanceRowResponse(
                rs.getLong("bundle_id"),
                rs.getString("bundle_name"),
                rs.getInt("units_sold"),
                rs.getBigDecimal("revenue"),
                rs.getBigDecimal("cost"),
                rs.getBigDecimal("profit"),
                rs.getBigDecimal("margin"),
                rs.getInt("sales_count")
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteRentabilidadRowResponse> clientesRentabilidad(int days, int limit) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        ZoneId zone = resolveUserZone();
        Integer safeDays = normalizeDays(days);
        int safeLimit = Math.max(1, Math.min(limit, 100));

        String sqlBase = """
                SELECT
                    c.id AS client_id,
                    c.name AS client_name,
                    COUNT(*) AS sales_count,
                    SUM(sa.units_sold) AS units_sold,
                    ROUND(SUM(sa.revenue), 2) AS revenue,
                    ROUND(SUM(sa.cost), 2) AS cost,
                    ROUND(SUM(sa.profit), 2) AS profit,
                    CASE
                        WHEN SUM(sa.revenue) = 0 THEN 0
                        ELSE ROUND((SUM(sa.profit) / SUM(sa.revenue)) * 100, 2)
                    END AS margin,
                    MAX(s.sold_at) AS last_sale_at,
                    ROUND(AVG(s.total_amount), 2) AS avg_ticket
                FROM sales s
                JOIN clients c ON c.id = s.client_id
                JOIN (
                    SELECT
                        si.sale_id,
                        SUM(si.quantity) AS units_sold,
                        SUM(si.quantity * si.unit_price_snapshot) AS revenue,
                        SUM(si.quantity * si.cost_price_snapshot) AS cost,
                        SUM(si.quantity * (si.unit_price_snapshot - si.cost_price_snapshot)) AS profit
                    FROM sale_items si
                    GROUP BY si.sale_id
                ) sa ON sa.sale_id = s.id
                WHERE s.organization_id = :organizationId
                """;
        String sql = safeDays == null
                ? sqlBase + """
                GROUP BY c.id, c.name
                ORDER BY revenue DESC, client_name
                LIMIT :limit
                """
                : sqlBase + """
                  AND s.sold_at >= :dateFrom
                GROUP BY c.id, c.name
                ORDER BY revenue DESC, client_name
                LIMIT :limit
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("organizationId", organizationId)
                .addValue("limit", safeLimit);
        if (safeDays != null) {
            params.addValue("dateFrom", toUtcStartOfDayTimestamp(zone, safeDays));
        }

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new ClienteRentabilidadRowResponse(
                rs.getLong("client_id"),
                rs.getString("client_name"),
                rs.getInt("sales_count"),
                rs.getInt("units_sold"),
                rs.getBigDecimal("revenue"),
                rs.getBigDecimal("cost"),
                rs.getBigDecimal("profit"),
                rs.getBigDecimal("margin"),
                rs.getTimestamp("last_sale_at").toLocalDateTime(),
                rs.getBigDecimal("avg_ticket")
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorPerformanceRowResponse> proveedoresPerformance(int days, int limit) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        ZoneId zone = resolveUserZone();
        Integer safeDays = normalizeDays(days);
        int safeLimit = Math.max(1, Math.min(limit, 100));

        String sqlBase = """
                SELECT
                    p.id AS provider_id,
                    p.name AS provider_name,
                    COUNT(po.id) AS order_count,
                    ROUND(SUM(po.order_spend), 2) AS total_spend,
                    ROUND(AVG(
                        CASE
                            WHEN po.received_at IS NULL THEN NULL
                            ELSE DATEDIFF(po.received_at, po.created_at)
                        END
                    ), 2) AS avg_lead_time_days,
                    CASE
                        WHEN SUM(CASE WHEN po.received_at IS NOT NULL THEN 1 ELSE 0 END) = 0 THEN 0
                        ELSE ROUND(
                            (
                                SUM(
                                    CASE
                                        WHEN po.received_at IS NOT NULL
                                         AND po.received_at <= TIMESTAMP(po.desired_arrival_date, '23:59:59')
                                        THEN 1 ELSE 0
                                    END
                                ) * 100.0
                            ) / SUM(CASE WHEN po.received_at IS NOT NULL THEN 1 ELSE 0 END),
                            2
                        )
                    END AS on_time_pct,
                    MAX(po.created_at) AS last_order_at
                FROM providers p
                JOIN (
                    SELECT
                        po.id,
                        po.provider_id,
                        po.organization_id,
                        po.created_at,
                        po.desired_arrival_date,
                        po.received_at,
                        COALESCE(SUM(poi.quantity * poi.unit_price_snapshot), 0) AS order_spend
                    FROM purchase_orders po
                    LEFT JOIN purchase_order_items poi ON poi.order_id = po.id
                    WHERE po.organization_id = :organizationId
                    GROUP BY po.id, po.provider_id, po.organization_id, po.created_at, po.desired_arrival_date, po.received_at
                ) po ON po.provider_id = p.id
                WHERE p.organization_id = :organizationId
                """;
        String sql = safeDays == null
                ? sqlBase + """
                GROUP BY p.id, p.name
                ORDER BY total_spend DESC, provider_name
                LIMIT :limit
                """
                : sqlBase + """
                  AND po.created_at >= :dateFrom
                GROUP BY p.id, p.name
                ORDER BY total_spend DESC, provider_name
                LIMIT :limit
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("organizationId", organizationId)
                .addValue("limit", safeLimit);
        if (safeDays != null) {
            params.addValue("dateFrom", toUtcStartOfDayTimestamp(zone, safeDays));
        }

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new ProveedorPerformanceRowResponse(
                rs.getLong("provider_id"),
                rs.getString("provider_name"),
                rs.getInt("order_count"),
                rs.getBigDecimal("total_spend"),
                rs.getBigDecimal("avg_lead_time_days"),
                rs.getBigDecimal("on_time_pct"),
                rs.getTimestamp("last_order_at").toLocalDateTime()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertaSerieRowResponse> alertasSerie(int days) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        ZoneId zone = resolveUserZone();
        String userTimeZone = zone.getId();
        Integer safeDays = normalizeDays(days);

        String sql = """
                SELECT
                    t.bucket_date,
                    t.alert_type,
                    SUM(t.created_count) AS created_count,
                    SUM(t.resolved_count) AS resolved_count
                FROM (
                    SELECT
                        DATE(CONVERT_TZ(sa.created_at, '+00:00', :userTz)) AS bucket_date,
                        sa.alert_type,
                        COUNT(*) AS created_count,
                        0 AS resolved_count
                    FROM stock_alerts sa
                    WHERE sa.organization_id = :organizationId
                    %s
                    GROUP BY DATE(CONVERT_TZ(sa.created_at, '+00:00', :userTz)), sa.alert_type

                    UNION ALL

                    SELECT
                        DATE(CONVERT_TZ(sa.resolved_at, '+00:00', :userTz)) AS bucket_date,
                        sa.alert_type,
                        0 AS created_count,
                        COUNT(*) AS resolved_count
                    FROM stock_alerts sa
                    WHERE sa.organization_id = :organizationId
                      AND sa.resolved_at IS NOT NULL
                    %s
                    GROUP BY DATE(CONVERT_TZ(sa.resolved_at, '+00:00', :userTz)), sa.alert_type
                ) t
                GROUP BY t.bucket_date, t.alert_type
                ORDER BY t.bucket_date, t.alert_type
                """.formatted(
                safeDays == null ? "" : "AND sa.created_at >= :dateFrom",
                safeDays == null ? "" : "AND sa.resolved_at >= :dateFrom"
        );

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("organizationId", organizationId)
                .addValue("userTz", userTimeZone);
        if (safeDays != null) {
            params.addValue("dateFrom", toUtcStartOfDayTimestamp(zone, safeDays));
        }

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new AlertaSerieRowResponse(
                rs.getDate("bucket_date").toLocalDate(),
                rs.getString("alert_type"),
                rs.getLong("created_count"),
                rs.getLong("resolved_count")
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCreationRowResponse> productosCreadosTimeline(int days) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        ZoneId zone = resolveUserZone();
        String userTimeZone = zone.getId();
        Integer safeDays = normalizeDays(days);
        String sqlBase = """
                SELECT DATE(CONVERT_TZ(p.created_at, '+00:00', :userTz)) AS creation_date,
                       COUNT(*) AS products_created
                FROM products p
                WHERE p.organization_id = :organizationId
                """;
        String sql = safeDays == null
                ? sqlBase + "\nGROUP BY DATE(CONVERT_TZ(p.created_at, '+00:00', :userTz))\nORDER BY creation_date"
                : sqlBase + "\n  AND p.created_at >= :dateFrom\nGROUP BY DATE(CONVERT_TZ(p.created_at, '+00:00', :userTz))\nORDER BY creation_date";

        MapSqlParameterSource params = new MapSqlParameterSource("organizationId", organizationId)
                .addValue("userTz", userTimeZone);
        if (safeDays != null) {
            params.addValue("dateFrom", toUtcStartOfDayTimestamp(zone, safeDays));
        }

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new ProductCreationRowResponse(
                rs.getDate("creation_date").toLocalDate(),
                rs.getLong("products_created")
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockReceiptRowResponse> stockRecibidoTimeline(int days) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        ZoneId zone = resolveUserZone();
        String userTimeZone = zone.getId();
        Integer safeDays = normalizeDays(days);
        String sqlBase = """
                SELECT DATE(CONVERT_TZ(sm.moved_at, '+00:00', :userTz)) AS receipt_date,
                       COALESCE(SUM(sm.delta), 0) AS units_received,
                       ROUND(COALESCE(SUM(sm.delta * COALESCE(p.acquisition_price, p.unit_price)), 0), 2) AS value_received
                FROM stock_movements sm
                JOIN products p ON p.id = sm.product_id
                WHERE sm.organization_id = :organizationId
                  AND sm.reason = 'PURCHASE'
                """;
        String sql = safeDays == null
                ? sqlBase + "\nGROUP BY DATE(CONVERT_TZ(sm.moved_at, '+00:00', :userTz))\nORDER BY receipt_date"
                : sqlBase + "\n  AND sm.moved_at >= :dateFrom\nGROUP BY DATE(CONVERT_TZ(sm.moved_at, '+00:00', :userTz))\nORDER BY receipt_date";

        MapSqlParameterSource params = new MapSqlParameterSource("organizationId", organizationId)
                .addValue("userTz", userTimeZone);
        if (safeDays != null) {
            params.addValue("dateFrom", toUtcStartOfDayTimestamp(zone, safeDays));
        }

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new StockReceiptRowResponse(
                rs.getDate("receipt_date").toLocalDate(),
                rs.getLong("units_received"),
                rs.getBigDecimal("value_received")
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public AnaliticaMetaResponse metadata(int days) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        ZoneId zone = resolveUserZone();
        Integer safeDays = normalizeDays(days);
        int resolvedWindowDays = safeDays == null ? 0 : safeDays;
        MapSqlParameterSource params = new MapSqlParameterSource("organizationId", organizationId);
        if (safeDays != null) {
            params.addValue("dateFrom", toUtcStartOfDayTimestamp(zone, safeDays));
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
                .orElse(LocalDateTime.now(zone));

        boolean seedApplied = isAnalyticsSeedApplied();
        String source = analyticsSeedEnabled
                ? (seedApplied ? "API_REAL_LOCAL_SEEDED" : "API_REAL_SEED_PENDING")
                : "API_REAL";

        return new AnaliticaMetaResponse(source, analyticsSeedEnabled, generatedAt, resolvedWindowDays);
    }

    private RevenueTimelineSpec resolveRevenueTimelineSpec(Long organizationId, int days, LocalDateTime now, ZoneId zone) {
        if (days == 1) {
            LocalDateTime start = now.toLocalDate().atStartOfDay();
            return new RevenueTimelineSpec(
                    RevenueGranularity.HOUR,
                    start,
                    now,
                    now.getHour() + 1,
                    1,
                    "Hoy",
                    "Mismo día anterior"
            );
        }

        if (days == 7) {
            int daysFromMonday = now.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
            LocalDateTime start = now.toLocalDate().minusDays(daysFromMonday).atStartOfDay();
            return new RevenueTimelineSpec(
                    RevenueGranularity.DAY,
                    start,
                    now,
                    daysFromMonday + 1,
                    7,
                    "Semana actual",
                    "Misma semana año anterior"
            );
        }

        if (days == 30) {
            LocalDateTime start = now.toLocalDate().minusDays(29).atStartOfDay();
            return new RevenueTimelineSpec(
                    RevenueGranularity.DAY,
                    start,
                    now,
                    30,
                    30,
                    "Últimos 30 días",
                    "Mismo periodo año anterior"
            );
        }

        if (days == 90) {
            LocalDateTime start = now.toLocalDate().minusDays((12L * 7L) - 1).atStartOfDay();
            return new RevenueTimelineSpec(
                    RevenueGranularity.WEEK,
                    start,
                    now,
                    12,
                    90,
                    "Últimas 12 semanas",
                    "Mismo periodo año anterior"
            );
        }

        if (days == 180) {
            LocalDateTime start = now.toLocalDate().minusDays((24L * 7L) - 1).atStartOfDay();
            return new RevenueTimelineSpec(
                    RevenueGranularity.WEEK,
                    start,
                    now,
                    24,
                    180,
                    "Últimas 24 semanas",
                    "Mismo periodo año anterior"
            );
        }

        Map<String, Object> minRow = jdbcTemplate.queryForMap(
                """
                        SELECT MIN(s.sold_at) AS min_sold_at
                        FROM sales s
                        WHERE s.organization_id = :organizationId
                        """,
                new MapSqlParameterSource("organizationId", organizationId)
        );
        LocalDateTime minSoldAtUtc = asLocalDateTime(minRow.get("min_sold_at"));
        if (minSoldAtUtc == null) {
            return new RevenueTimelineSpec(
                    RevenueGranularity.DAY,
                    now.toLocalDate().atStartOfDay(),
                    now,
                    0,
                    0,
                    "Histórico",
                    "Mismo periodo año anterior"
                );
        }
        LocalDateTime minSoldAt = minSoldAtUtc.atZone(ZoneId.of("UTC")).withZoneSameInstant(zone).toLocalDateTime();

        long spanDays = Math.max(0L, ChronoUnit.DAYS.between(minSoldAt.toLocalDate(), now.toLocalDate()));
        RevenueGranularity granularity = spanDays >= 730
                ? RevenueGranularity.YEAR
                : spanDays >= 90
                ? RevenueGranularity.MONTH
                : spanDays >= 21
                ? RevenueGranularity.WEEK
                : spanDays >= 1
                ? RevenueGranularity.DAY
                : RevenueGranularity.HOUR;

        LocalDateTime start = truncateToGranularity(minSoldAt, granularity);
        int bucketCount = resolveBucketCount(start, now, granularity);

        return new RevenueTimelineSpec(
                granularity,
                start,
                now,
                bucketCount,
                0,
                "Histórico",
                "Mismo periodo año anterior"
        );
    }

    private int resolveBucketCount(LocalDateTime start, LocalDateTime end, RevenueGranularity granularity) {
        if (end.isBefore(start)) return 0;
        return switch (granularity) {
            case HOUR -> Math.toIntExact(ChronoUnit.HOURS.between(start, end)) + 1;
            case DAY -> Math.toIntExact(ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate())) + 1;
            case WEEK -> (Math.toIntExact(ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate())) / 7) + 1;
            case MONTH -> Math.toIntExact(ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(end))) + 1;
            case YEAR -> end.getYear() - start.getYear() + 1;
        };
    }

    private LocalDateTime truncateToGranularity(LocalDateTime dateTime, RevenueGranularity granularity) {
        return switch (granularity) {
            case HOUR -> dateTime.withMinute(0).withSecond(0).withNano(0);
            case DAY -> dateTime.toLocalDate().atStartOfDay();
            case WEEK -> {
                LocalDate date = dateTime.toLocalDate();
                int daysFromMonday = date.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
                yield date.minusDays(daysFromMonday).atStartOfDay();
            }
            case MONTH -> dateTime.toLocalDate().withDayOfMonth(1).atStartOfDay();
            case YEAR -> dateTime.toLocalDate().withDayOfYear(1).atStartOfDay();
        };
    }

    private int resolveBucketIndex(
            LocalDateTime value,
            LocalDateTime bucketStart,
            RevenueGranularity granularity,
            int bucketCount
    ) {
        int index = switch (granularity) {
            case HOUR -> Math.toIntExact(ChronoUnit.HOURS.between(bucketStart, value));
            case DAY -> Math.toIntExact(ChronoUnit.DAYS.between(bucketStart.toLocalDate(), value.toLocalDate()));
            case WEEK -> Math.toIntExact(ChronoUnit.DAYS.between(bucketStart.toLocalDate(), value.toLocalDate())) / 7;
            case MONTH -> Math.toIntExact(ChronoUnit.MONTHS.between(YearMonth.from(bucketStart), YearMonth.from(value)));
            case YEAR -> value.getYear() - bucketStart.getYear();
        };
        if (index < 0 || index >= bucketCount) return -1;
        return index;
    }

    private LocalDateTime resolveBucketStart(LocalDateTime start, RevenueGranularity granularity, int index) {
        return switch (granularity) {
            case HOUR -> start.plusHours(index);
            case DAY -> start.plusDays(index);
            case WEEK -> start.plusDays((long) index * 7L);
            case MONTH -> start.plusMonths(index);
            case YEAR -> start.plusYears(index);
        };
    }

    private String resolveBucketLabel(LocalDateTime start, RevenueGranularity granularity, int requestedDays, int index) {
        return switch (granularity) {
            case HOUR -> String.format("%02d:00", start.getHour());
            case DAY -> requestedDays == 7
                    ? start.toLocalDate().getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, new Locale("es", "ES")).replace(".", "")
                    : String.format("%02d/%02d", start.getDayOfMonth(), start.getMonthValue());
            case WEEK -> "S" + (index + 1);
            case MONTH -> start.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, new Locale("es", "ES")).replace(".", "") + " " + start.getYear();
            case YEAR -> String.valueOf(start.getYear());
        };
    }

    private enum RevenueGranularity {
        HOUR,
        DAY,
        WEEK,
        MONTH,
        YEAR
    }

    private record RevenueTimelineSpec(
            RevenueGranularity granularity,
            LocalDateTime currentStart,
            LocalDateTime currentEnd,
            int bucketCount,
            int requestedDays,
            String currentLabel,
            String previousLabel
    ) {
    }

    private record SaleRevenuePoint(LocalDateTime soldAt, BigDecimal amount) {
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

    private ZoneId resolveUserZone() {
        return ZoneId.of(currentUserResolver.requireUser().timeZone());
    }

    private Timestamp toUtcStartOfDayTimestamp(ZoneId zone, int daysBack) {
        Instant instant = LocalDate.now(zone).minusDays(daysBack).atStartOfDay(zone).toInstant();
        return Timestamp.from(instant);
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
