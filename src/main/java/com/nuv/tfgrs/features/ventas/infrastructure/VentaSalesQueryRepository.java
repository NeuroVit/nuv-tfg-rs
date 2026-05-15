package com.nuv.tfgrs.features.ventas.infrastructure;

import com.nuv.tfgrs.features.ventas.dto.response.VentaItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class VentaSalesQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<VentaHeaderRow> fetchSalesHeaders(Long clientId, int limit, Long organizationId) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    s.id,
                    s.client_id,
                    c.name AS client_name,
                    s.sold_at,
                    s.notes,
                    s.subtotal_amount,
                    s.discount_total_amount,
                    s.global_discount_pct,
                    s.total_amount,
                    s.created_at
                FROM sales s
                JOIN clients c ON c.id = s.client_id
                """);

        MapSqlParameterSource params = new MapSqlParameterSource("organizationId", organizationId);
        sql.append(" WHERE s.organization_id = :organizationId ");
        if (clientId != null) {
            sql.append(" AND s.client_id = :clientId ");
            params.addValue("clientId", clientId);
        }

        sql.append(" ORDER BY s.sold_at DESC LIMIT :limit");
        params.addValue("limit", limit);

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> new VentaHeaderRow(
                rs.getLong("id"),
                rs.getLong("client_id"),
                rs.getString("client_name"),
                rs.getTimestamp("sold_at").toLocalDateTime(),
                rs.getString("notes"),
                rs.getBigDecimal("subtotal_amount"),
                rs.getBigDecimal("discount_total_amount"),
                rs.getBigDecimal("global_discount_pct"),
                rs.getBigDecimal("total_amount"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ));
    }

    public List<VentaHeaderRow> fetchSalesHeadersPaged(
            Long clientId,
            String search,
            LocalDate fromDate,
            LocalDate toDate,
            int limit,
            int offset,
            Long organizationId
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    s.id,
                    s.client_id,
                    c.name AS client_name,
                    s.sold_at,
                    s.notes,
                    s.subtotal_amount,
                    s.discount_total_amount,
                    s.global_discount_pct,
                    s.total_amount,
                    s.created_at
                FROM sales s
                JOIN clients c ON c.id = s.client_id
                """);

        MapSqlParameterSource params = new MapSqlParameterSource("organizationId", organizationId);
        appendSalesFilters(sql, params, clientId, search, fromDate, toDate);
        sql.append(" ORDER BY s.sold_at DESC, s.id DESC LIMIT :limit OFFSET :offset");
        params.addValue("limit", limit);
        params.addValue("offset", offset);

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> new VentaHeaderRow(
                rs.getLong("id"),
                rs.getLong("client_id"),
                rs.getString("client_name"),
                rs.getTimestamp("sold_at").toLocalDateTime(),
                rs.getString("notes"),
                rs.getBigDecimal("subtotal_amount"),
                rs.getBigDecimal("discount_total_amount"),
                rs.getBigDecimal("global_discount_pct"),
                rs.getBigDecimal("total_amount"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ));
    }

    public long countSalesHeaders(
            Long clientId,
            String search,
            LocalDate fromDate,
            LocalDate toDate,
            Long organizationId
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM sales s
                JOIN clients c ON c.id = s.client_id
                """);
        MapSqlParameterSource params = new MapSqlParameterSource("organizationId", organizationId);
        appendSalesFilters(sql, params, clientId, search, fromDate, toDate);
        Long total = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        return total == null ? 0 : total;
    }

    public VentaHeaderRow fetchSaleHeaderById(Long saleId, Long organizationId) {
        String sql = """
                SELECT
                    s.id,
                    s.client_id,
                    c.name AS client_name,
                    s.sold_at,
                    s.notes,
                    s.subtotal_amount,
                    s.discount_total_amount,
                    s.global_discount_pct,
                    s.total_amount,
                    s.created_at
                FROM sales s
                JOIN clients c ON c.id = s.client_id
                WHERE s.id = :saleId
                  AND s.organization_id = :organizationId
                """;

        List<VentaHeaderRow> rows = jdbcTemplate.query(sql, new MapSqlParameterSource("saleId", saleId)
                        .addValue("organizationId", organizationId), (rs, rowNum) ->
                new VentaHeaderRow(
                        rs.getLong("id"),
                        rs.getLong("client_id"),
                        rs.getString("client_name"),
                        rs.getTimestamp("sold_at").toLocalDateTime(),
                        rs.getString("notes"),
                        rs.getBigDecimal("subtotal_amount"),
                        rs.getBigDecimal("discount_total_amount"),
                        rs.getBigDecimal("global_discount_pct"),
                        rs.getBigDecimal("total_amount"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                )
        );

        if (rows.isEmpty()) {
            return null;
        }

        return rows.get(0);
    }

    public Map<Long, List<VentaItemResponse>> fetchSaleItems(List<Long> saleIds, Long organizationId) {
        if (saleIds.isEmpty()) {
            return Map.of();
        }

        String sql = """
                SELECT
                    si.sale_id,
                    si.product_id,
                    si.bundle_id,
                    b.name AS bundle_name,
                    p.name AS product_name,
                    p.sku,
                    si.quantity,
                    si.list_unit_price_snapshot,
                    si.discount_pct_snapshot,
                    si.unit_price_snapshot
                FROM sale_items si
                JOIN products p ON p.id = si.product_id
                LEFT JOIN bundles b
                  ON b.id = si.bundle_id
                 AND b.organization_id = si.organization_id
                WHERE si.sale_id IN (:saleIds)
                  AND si.organization_id = :organizationId
                ORDER BY p.name
                """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, new MapSqlParameterSource("saleIds", saleIds)
                .addValue("organizationId", organizationId));
        Map<Long, List<VentaItemResponse>> grouped = new HashMap<>();

        for (Map<String, Object> row : rows) {
            Long saleId = ((Number) row.get("sale_id")).longValue();
            Long productId = ((Number) row.get("product_id")).longValue();
            Long bundleId = asLong(row.get("bundle_id"));
            Integer quantity = ((Number) row.get("quantity")).intValue();
            BigDecimal listUnitPrice = asBigDecimal(row.get("list_unit_price_snapshot"));
            BigDecimal discountPct = asBigDecimal(row.get("discount_pct_snapshot"));
            BigDecimal unitPrice = asBigDecimal(row.get("unit_price_snapshot"));

            grouped.computeIfAbsent(saleId, ignored -> new ArrayList<>()).add(new VentaItemResponse(
                    productId,
                    bundleId,
                    (String) row.get("bundle_name"),
                    (String) row.get("product_name"),
                    (String) row.get("sku"),
                    quantity,
                    listUnitPrice,
                    discountPct,
                    unitPrice,
                    unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP)
            ));
        }

        return grouped;
    }

    private void appendSalesFilters(
            StringBuilder sql,
            MapSqlParameterSource params,
            Long clientId,
            String search,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        sql.append(" WHERE s.organization_id = :organizationId ");
        if (clientId != null) {
            sql.append(" AND s.client_id = :clientId ");
            params.addValue("clientId", clientId);
        }

        String normalizedSearch = normalizeLikeSearch(search);
        if (normalizedSearch != null) {
            sql.append(" AND (CAST(s.id AS CHAR) LIKE :search OR LOWER(COALESCE(s.notes, '')) LIKE :search) ");
            params.addValue("search", normalizedSearch);
        }

        if (fromDate != null) {
            sql.append(" AND s.sold_at >= :fromDate ");
            params.addValue("fromDate", Timestamp.valueOf(fromDate.atStartOfDay()));
        }
        if (toDate != null) {
            sql.append(" AND s.sold_at <= :toDate ");
            params.addValue("toDate", Timestamp.valueOf(toDate.plusDays(1).atStartOfDay().minusNanos(1)));
        }
    }

    private String normalizeLikeSearch(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) return null;
        return "%" + normalized + "%";
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return BigDecimal.ZERO;
    }

    private Long asLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        return null;
    }

    public record VentaHeaderRow(
            Long id,
            Long clientId,
            String clientName,
            LocalDateTime soldAt,
            String notes,
            BigDecimal subtotalAmount,
            BigDecimal discountTotalAmount,
            BigDecimal globalDiscountPct,
            BigDecimal totalAmount,
            LocalDateTime createdAt
    ) {
    }
}
