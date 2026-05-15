package com.nuv.tfgrs.features.analitica.infrastructure;

import com.nuv.tfgrs.features.analitica.dto.response.InventorySummaryRowResponse;
import com.nuv.tfgrs.features.common.dto.response.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AnaliticaInventoryQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<InventorySummaryRowResponse> findInventorySummary(Long organizationId) {
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

    public PagedResponse<InventorySummaryRowResponse> findInventorySummaryPaged(Long organizationId, int limit, int offset) {
        String countSql = """
                SELECT COUNT(*)
                FROM v_inventory_summary v
                JOIN products p ON p.id = v.product_id
                WHERE p.organization_id = :organizationId
                """;
        Long total = jdbcTemplate.queryForObject(
                countSql,
                new MapSqlParameterSource("organizationId", organizationId),
                Long.class
        );

        String dataSql = """
                SELECT v.product_id, v.sku, v.product_name, v.category_name, v.current_quantity, v.minimum_stock_level,
                       v.unit_price, v.stock_value, v.stock_status
                FROM v_inventory_summary v
                JOIN products p ON p.id = v.product_id
                WHERE p.organization_id = :organizationId
                ORDER BY v.stock_value DESC
                LIMIT :limit OFFSET :offset
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("organizationId", organizationId)
                .addValue("limit", limit)
                .addValue("offset", offset);
        List<InventorySummaryRowResponse> content = jdbcTemplate.query(dataSql, params, (rs, rowNum) -> new InventorySummaryRowResponse(
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

        return new PagedResponse<>(content, total == null ? 0 : total, limit, offset);
    }
}
