package com.nuv.tfgrs.features.analitica.service.impl;

import com.nuv.tfgrs.features.analitica.dto.response.VelocidadResumenRowResponse;
import com.nuv.tfgrs.features.analitica.service.AnaliticaService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AnaliticaServiceImplTest {

    @Autowired
    private AnaliticaService analiticaService;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void velocidadResumenDebeRespetarZonaHorariaDelUsuario() {
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = :username",
                new MapSqlParameterSource("username", "admin"),
                Long.class
        );
        Assertions.assertNotNull(userId);

        Long organizationId = jdbcTemplate.queryForObject(
                "SELECT organization_id FROM organization_memberships WHERE user_id = :userId LIMIT 1",
                new MapSqlParameterSource("userId", userId),
                Long.class
        );
        Assertions.assertNotNull(organizationId);

        Long clientId = insertClient(organizationId);
        Long categoryId = insertCategory(organizationId);
        Long providerId = insertProvider(organizationId);
        Long productId = insertProduct(organizationId, categoryId, providerId);
        Long saleId = insertSale(organizationId, clientId);
        insertSaleItem(organizationId, saleId, productId);

        jdbcTemplate.update(
                "UPDATE users SET time_zone = :tz WHERE id = :id",
                new MapSqlParameterSource().addValue("tz", "Europe/Madrid").addValue("id", userId)
        );

        List<VelocidadResumenRowResponse> madridRows = analiticaService.velocidadResumen(7);
        VelocidadResumenRowResponse madridBucket = madridRows.stream()
                .filter(row -> LocalDate.of(2026, 5, 14).equals(row.movementDate()))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(madridBucket, "Debe existir bucket del 2026-05-14 para Europe/Madrid");
        Assertions.assertTrue(
                madridBucket.revenue().compareTo(new BigDecimal("987654.32")) >= 0,
                "El bucket de Europe/Madrid debe incluir la venta sembrada"
        );

        jdbcTemplate.update(
                "UPDATE users SET time_zone = :tz WHERE id = :id",
                new MapSqlParameterSource().addValue("tz", "America/Los_Angeles").addValue("id", userId)
        );

        List<VelocidadResumenRowResponse> laRows = analiticaService.velocidadResumen(7);
        VelocidadResumenRowResponse laBucket = laRows.stream()
                .filter(row -> LocalDate.of(2026, 5, 13).equals(row.movementDate()))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(laBucket, "Debe existir bucket del 2026-05-13 para America/Los_Angeles");
        Assertions.assertTrue(
                laBucket.revenue().compareTo(new BigDecimal("987654.32")) >= 0,
                "El bucket de America/Los_Angeles debe incluir la venta sembrada"
        );
    }

    private Long insertClient(Long organizationId) {
        String sql = """
                INSERT INTO clients (organization_id, name, email, phone, notes, is_active, created_at, updated_at)
                VALUES (:organizationId, :name, :email, :phone, :notes, :active, NOW(), NOW())
                """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        String suffix = String.valueOf(System.nanoTime());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("organizationId", organizationId)
                .addValue("name", "TZ Client " + suffix)
                .addValue("email", "tz-client-" + suffix + "@test.local")
                .addValue("phone", "600000000")
                .addValue("notes", "tz test")
                .addValue("active", true);
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        Assertions.assertNotNull(key);
        return key.longValue();
    }

    private Long insertCategory(Long organizationId) {
        String sql = """
                INSERT INTO categories (organization_id, name, description, created_at)
                VALUES (:organizationId, :name, :description, NOW())
                """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        String suffix = String.valueOf(System.nanoTime());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("organizationId", organizationId)
                .addValue("name", "TZ Category " + suffix)
                .addValue("description", "tz test");
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        Assertions.assertNotNull(key);
        return key.longValue();
    }

    private Long insertProvider(Long organizationId) {
        String sql = """
                INSERT INTO providers (
                    organization_id, name, email, warehouse_location, lead_time_days, is_active, created_at, updated_at
                )
                VALUES (:organizationId, :name, :email, :warehouse, :leadTime, :active, NOW(), NOW())
                """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        String suffix = String.valueOf(System.nanoTime());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("organizationId", organizationId)
                .addValue("name", "TZ Provider " + suffix)
                .addValue("email", "tz-provider-" + suffix + "@test.local")
                .addValue("warehouse", "tz warehouse")
                .addValue("leadTime", 3)
                .addValue("active", true);
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        Assertions.assertNotNull(key);
        return key.longValue();
    }

    private Long insertProduct(Long organizationId, Long categoryId, Long providerId) {
        String sql = """
                INSERT INTO products (
                    organization_id, name, sku, description, current_quantity, minimum_stock_level,
                    unit_price, acquisition_price, category_id, provider_id, created_at, updated_at
                )
                VALUES (
                    :organizationId, :name, :sku, :description, :quantity, :minimum,
                    :unitPrice, :acquisitionPrice, :categoryId, :providerId, NOW(), NOW()
                )
                """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        String suffix = String.valueOf(System.nanoTime());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("organizationId", organizationId)
                .addValue("name", "TZ Product " + suffix)
                .addValue("sku", "TZ-SKU-" + suffix)
                .addValue("description", "tz test")
                .addValue("quantity", 100)
                .addValue("minimum", 10)
                .addValue("unitPrice", new BigDecimal("987654.32"))
                .addValue("acquisitionPrice", new BigDecimal("500000.00"))
                .addValue("categoryId", categoryId)
                .addValue("providerId", providerId);
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        Assertions.assertNotNull(key);
        return key.longValue();
    }

    private Long insertSale(Long organizationId, Long clientId) {
        String sql = """
                INSERT INTO sales (
                    organization_id, client_id, sold_at, notes, subtotal_amount, discount_total_amount,
                    global_discount_pct, total_amount, created_by, created_at
                )
                VALUES (
                    :organizationId, :clientId, :soldAt, :notes, :subtotalAmount, :discountAmount,
                    :discountPct, :totalAmount, NULL, :createdAt
                )
                """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        Timestamp soldAt = Timestamp.valueOf("2026-05-13 23:30:00");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("organizationId", organizationId)
                .addValue("clientId", clientId)
                .addValue("soldAt", soldAt)
                .addValue("notes", "timezone test")
                .addValue("subtotalAmount", new BigDecimal("987654.32"))
                .addValue("discountAmount", BigDecimal.ZERO)
                .addValue("discountPct", BigDecimal.ZERO)
                .addValue("totalAmount", new BigDecimal("987654.32"))
                .addValue("createdAt", soldAt);
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        Assertions.assertNotNull(key);
        return key.longValue();
    }

    private void insertSaleItem(Long organizationId, Long saleId, Long productId) {
        String sql = """
                INSERT INTO sale_items (
                    sale_id, organization_id, product_id, quantity,
                    list_unit_price_snapshot, discount_pct_snapshot, unit_price_snapshot, cost_price_snapshot
                )
                VALUES (
                    :saleId, :organizationId, :productId, :quantity,
                    :listUnitPrice, :discountPct, :unitPrice, :costPrice
                )
                """;

        jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("saleId", saleId)
                        .addValue("organizationId", organizationId)
                        .addValue("productId", productId)
                        .addValue("quantity", 1)
                        .addValue("listUnitPrice", new BigDecimal("987654.32"))
                        .addValue("discountPct", BigDecimal.ZERO)
                        .addValue("unitPrice", new BigDecimal("987654.32"))
                        .addValue("costPrice", new BigDecimal("500000.00"))
        );
    }
}
