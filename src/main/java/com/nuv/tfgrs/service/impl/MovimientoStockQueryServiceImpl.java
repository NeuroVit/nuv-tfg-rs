package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.dto.response.MovimientoStockPageResponse;
import com.nuv.tfgrs.dto.response.MovimientoStockResponse;
import com.nuv.tfgrs.security.org.CurrentOrganizationResolver;
import com.nuv.tfgrs.service.MovimientoStockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovimientoStockQueryServiceImpl implements MovimientoStockQueryService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CurrentOrganizationResolver currentOrganizationResolver;

    @Override
    @Transactional(readOnly = true)
    public MovimientoStockPageResponse listar(
            Long productId,
            String tipo,
            String razon,
            LocalDate desde,
            LocalDate hasta,
            Integer limit,
            Integer offset
    ) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        int safeLimit = Math.max(1, Math.min(limit == null ? 20 : limit, 1000));
        int safeOffset = Math.max(0, offset == null ? 0 : offset);

        String normalizedTipo = tipo == null ? null : tipo.trim().toUpperCase();
        String normalizedRazon = razon == null ? null : razon.trim().toUpperCase();

        List<String> filters = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource("organizationId", organizationId);
        filters.add("sm.organization_id = :organizationId");

        if (productId != null) {
            filters.add("sm.product_id = :productId");
            params.addValue("productId", productId);
        }

        if ("ENTRADA".equals(normalizedTipo)) {
            filters.add("sm.delta >= 0");
        } else if ("SALIDA".equals(normalizedTipo)) {
            filters.add("sm.delta < 0");
        }

        if (normalizedRazon != null && !normalizedRazon.isBlank()) {
            filters.add("sm.reason = :reason");
            params.addValue("reason", normalizedRazon);
        }

        if (desde != null) {
            filters.add("sm.moved_at >= :fromDate");
            params.addValue("fromDate", Timestamp.valueOf(desde.atStartOfDay()));
        }

        if (hasta != null) {
            filters.add("sm.moved_at <= :toDate");
            params.addValue("toDate", Timestamp.valueOf(hasta.plusDays(1).atStartOfDay().minusNanos(1)));
        }

        String whereClause = filters.isEmpty() ? "" : " WHERE " + String.join(" AND ", filters);

        String countSql = """
                SELECT COUNT(*)
                FROM stock_movements sm
                """ + whereClause;

        Long total = jdbcTemplate.queryForObject(countSql, params, Long.class);
        long totalRows = total == null ? 0 : total;

        String dataSql = """
                SELECT
                    sm.id,
                    p.name AS producto_nombre,
                    p.sku AS producto_sku,
                    ABS(sm.delta) AS cantidad,
                    CASE WHEN sm.delta >= 0 THEN 'ENTRADA' ELSE 'SALIDA' END AS tipo,
                    sm.reason,
                    sm.notes,
                    sm.moved_at
                FROM stock_movements sm
                JOIN products p ON p.id = sm.product_id
                """ + whereClause + " ORDER BY sm.moved_at DESC LIMIT :limit OFFSET :offset";

        params.addValue("limit", safeLimit);
        params.addValue("offset", safeOffset);

        List<MovimientoStockResponse> content = jdbcTemplate.query(dataSql, params, (rs, rowNum) ->
                new MovimientoStockResponse(
                        rs.getLong("id"),
                        rs.getString("producto_nombre"),
                        rs.getString("producto_sku"),
                        rs.getInt("cantidad"),
                        rs.getString("tipo"),
                        rs.getString("reason"),
                        rs.getString("notes"),
                        rs.getTimestamp("moved_at").toLocalDateTime()
                )
        );

        return new MovimientoStockPageResponse(content, totalRows, safeLimit, safeOffset);
    }
}
