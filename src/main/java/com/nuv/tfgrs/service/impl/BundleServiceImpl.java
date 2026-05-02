package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.dto.request.BundleUpsertRequest;
import com.nuv.tfgrs.dto.response.BundleItemResponse;
import com.nuv.tfgrs.dto.response.BundleSummaryResponse;
import com.nuv.tfgrs.exception.ResourceNotFoundException;
import com.nuv.tfgrs.service.BundleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BundleServiceImpl implements BundleService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<BundleSummaryResponse> listarBundles() {
        String sql = """
                SELECT
                    b.id,
                    b.name AS nombre,
                    b.description AS descripcion,
                    b.discount_pct AS descuento_pct,
                    b.active AS activo,
                    COUNT(bi.product_id) AS cantidad_productos,
                    b.updated_at AS actualizado_en
                FROM bundles b
                LEFT JOIN bundle_items bi ON bi.bundle_id = b.id
                GROUP BY b.id, b.name, b.description, b.discount_pct, b.active, b.updated_at
                ORDER BY b.name
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new BundleSummaryResponse(
                rs.getLong("id"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getBigDecimal("descuento_pct"),
                rs.getBoolean("activo"),
                rs.getInt("cantidad_productos"),
                rs.getTimestamp("actualizado_en").toLocalDateTime()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BundleItemResponse> listarItemsDeBundle(Long bundleId) {
        assertBundleExists(bundleId);

        String sql = """
                SELECT
                    p.id AS producto_id,
                    p.name AS producto_nombre,
                    p.sku AS sku,
                    bi.quantity AS cantidad
                FROM bundle_items bi
                JOIN products p ON p.id = bi.product_id
                WHERE bi.bundle_id = :bundleId
                ORDER BY p.name
                """;

        MapSqlParameterSource params = new MapSqlParameterSource("bundleId", bundleId);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new BundleItemResponse(
                rs.getLong("producto_id"),
                rs.getString("producto_nombre"),
                rs.getString("sku"),
                rs.getInt("cantidad")
        ));
    }

    @Override
    @Transactional
    public BundleSummaryResponse crear(BundleUpsertRequest request) {
        BundleData data = normalizeAndValidate(request);
        validarProductosExistentes(data.items());

        String sql = """
                INSERT INTO bundles (name, description, discount_pct, active, created_at, updated_at)
                VALUES (:name, :description, :discountPct, :active, NOW(), NOW())
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("name", data.nombre())
                .addValue("description", data.descripcion())
                .addValue("discountPct", data.descuentoPct())
                .addValue("active", data.activo());

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo crear el bundle");
        }

        Long bundleId = key.longValue();
        reemplazarItems(bundleId, data.items());
        return obtenerResumen(bundleId);
    }

    @Override
    @Transactional
    public BundleSummaryResponse actualizar(Long bundleId, BundleUpsertRequest request) {
        assertBundleExists(bundleId);
        BundleData data = normalizeAndValidate(request);
        validarProductosExistentes(data.items());

        String sql = """
                UPDATE bundles
                SET name = :name,
                    description = :description,
                    discount_pct = :discountPct,
                    active = :active,
                    updated_at = NOW()
                WHERE id = :bundleId
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("bundleId", bundleId)
                .addValue("name", data.nombre())
                .addValue("description", data.descripcion())
                .addValue("discountPct", data.descuentoPct())
                .addValue("active", data.activo());

        jdbcTemplate.update(sql, params);
        reemplazarItems(bundleId, data.items());
        return obtenerResumen(bundleId);
    }

    @Override
    @Transactional
    public void eliminar(Long bundleId) {
        String sql = "DELETE FROM bundles WHERE id = :bundleId";
        int affected = jdbcTemplate.update(sql, new MapSqlParameterSource("bundleId", bundleId));
        if (affected == 0) {
            throw new ResourceNotFoundException("Bundle no encontrado: " + bundleId);
        }
    }

    private void assertBundleExists(Long bundleId) {
        String sql = "SELECT COUNT(*) FROM bundles WHERE id = :bundleId";
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("bundleId", bundleId), Integer.class);
        if (count == null || count == 0) {
            throw new ResourceNotFoundException("Bundle no encontrado: " + bundleId);
        }
    }

    private BundleSummaryResponse obtenerResumen(Long bundleId) {
        String sql = """
                SELECT
                    b.id,
                    b.name AS nombre,
                    b.description AS descripcion,
                    b.discount_pct AS descuento_pct,
                    b.active AS activo,
                    COUNT(bi.product_id) AS cantidad_productos,
                    b.updated_at AS actualizado_en
                FROM bundles b
                LEFT JOIN bundle_items bi ON bi.bundle_id = b.id
                WHERE b.id = :bundleId
                GROUP BY b.id, b.name, b.description, b.discount_pct, b.active, b.updated_at
                """;

        MapSqlParameterSource params = new MapSqlParameterSource("bundleId", bundleId);
        List<BundleSummaryResponse> rows = jdbcTemplate.query(sql, params, (rs, rowNum) -> new BundleSummaryResponse(
                rs.getLong("id"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getBigDecimal("descuento_pct"),
                rs.getBoolean("activo"),
                rs.getInt("cantidad_productos"),
                rs.getTimestamp("actualizado_en").toLocalDateTime()
        ));

        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Bundle no encontrado: " + bundleId);
        }
        return rows.get(0);
    }

    private void reemplazarItems(Long bundleId, List<BundleItemData> items) {
        jdbcTemplate.update(
                "DELETE FROM bundle_items WHERE bundle_id = :bundleId",
                new MapSqlParameterSource("bundleId", bundleId)
        );

        MapSqlParameterSource[] batch = items.stream()
                .map(item -> new MapSqlParameterSource()
                        .addValue("bundleId", bundleId)
                        .addValue("productId", item.productoId())
                        .addValue("quantity", item.cantidad()))
                .toArray(MapSqlParameterSource[]::new);

        String insertSql = """
                INSERT INTO bundle_items (bundle_id, product_id, quantity)
                VALUES (:bundleId, :productId, :quantity)
                """;
        jdbcTemplate.batchUpdate(insertSql, batch);
    }

    private void validarProductosExistentes(List<BundleItemData> items) {
        Set<Long> ids = new HashSet<>();
        for (BundleItemData item : items) {
            if (!ids.add(item.productoId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede repetir un producto en el mismo bundle");
            }
        }

        String sql = "SELECT COUNT(*) FROM products WHERE id IN (:ids)";
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("ids", ids), Integer.class);
        if (count == null || count != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uno o más productos no existen");
        }
    }

    private BundleData normalizeAndValidate(BundleUpsertRequest request) {
        String nombre = request.nombre() != null ? request.nombre().trim() : "";
        if (nombre.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del bundle es obligatorio");
        }

        String descripcion = request.descripcion();
        if (descripcion != null) {
            descripcion = descripcion.trim();
            if (descripcion.isBlank()) {
                descripcion = null;
            }
        }

        BigDecimal descuento = request.descuentoPct();
        if (descuento == null || descuento.compareTo(BigDecimal.ZERO) < 0 || descuento.compareTo(new BigDecimal("100")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El descuento debe estar entre 0 y 100");
        }

        List<BundleItemData> items = request.items().stream()
                .map(item -> new BundleItemData(item.productoId(), item.cantidad()))
                .toList();

        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El bundle debe incluir al menos un producto");
        }

        return new BundleData(nombre, descripcion, descuento, request.activo(), items);
    }

    private record BundleItemData(Long productoId, Integer cantidad) {
    }

    private record BundleData(
            String nombre,
            String descripcion,
            BigDecimal descuentoPct,
            Boolean activo,
            List<BundleItemData> items
    ) {
    }
}
