package com.nuv.tfgrs.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class SalesIntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Autowired
    protected NamedParameterJdbcTemplate jdbcTemplate;

    protected Long insertClient(boolean active) {
        String sql = """
                INSERT INTO clients (organization_id, name, email, phone, notes, is_active, created_at, updated_at)
                VALUES (:organizationId, :name, :email, :phone, :notes, :active, NOW(), NOW())
                """;

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        String suffix = String.valueOf(System.nanoTime());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("organizationId", currentOrganizationIdForTestUser())
                .addValue("name", (active ? "Test Active Client " : "Test Inactive Client ") + suffix)
                .addValue("email", (active ? "active-" : "inactive-") + suffix + "@test.local")
                .addValue("phone", "600000000")
                .addValue("notes", "created by integration test")
                .addValue("active", active);

        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("No se pudo insertar cliente de prueba");
        }
        return key.longValue();
    }

    private Long currentOrganizationIdForTestUser() {
        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;
        if (username != null && !username.isBlank()) {
            Long orgId = jdbcTemplate.queryForObject(
                    """
                    SELECT om.organization_id
                    FROM organization_memberships om
                    JOIN users u ON u.id = om.user_id
                    WHERE u.username = :username
                    LIMIT 1
                    """,
                    new MapSqlParameterSource("username", username),
                    Long.class
            );
            if (orgId != null) {
                return orgId;
            }
        }

        Long fallback = jdbcTemplate.queryForObject(
                "SELECT id FROM organizations ORDER BY id LIMIT 1",
                new MapSqlParameterSource(),
                Long.class
        );
        Assertions.assertNotNull(fallback, "No existe organización de prueba");
        return fallback;
    }

    protected Map<String, Object> directItem(long productId, int quantity, BigDecimal descuentoPct) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("productId", productId);
        item.put("quantity", quantity);
        if (descuentoPct != null) {
            item.put("descuentoPct", descuentoPct);
        }
        return item;
    }

    protected Map<String, Object> bundleItem(long bundleId, int quantity, BigDecimal descuentoPctOverride) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("bundleId", bundleId);
        item.put("quantity", quantity);
        if (descuentoPctOverride != null) {
            item.put("descuentoPctOverride", descuentoPctOverride);
        }
        return item;
    }

    protected Map<String, Object> salePayload(
            long clientId,
            List<Map<String, Object>> items,
            List<Map<String, Object>> bundles,
            BigDecimal descuentoPctGlobal,
            String notas
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("clientId", clientId);
        if (notas != null) {
            payload.put("notas", notas);
        }
        if (items != null && !items.isEmpty()) {
            payload.put("items", items);
        }
        if (bundles != null && !bundles.isEmpty()) {
            payload.put("bundles", bundles);
        }
        if (descuentoPctGlobal != null) {
            payload.put("descuentoPctGlobal", descuentoPctGlobal);
        }
        return payload;
    }

    protected CreatedSale createSaleExpectCreated(Map<String, Object> payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        long saleId = body.path("id").asLong(0);
        Assertions.assertTrue(saleId > 0, "La respuesta de venta debe incluir id");
        return new CreatedSale(saleId, body);
    }

    protected MvcResult createSaleExpectStatus(Map<String, Object> payload, int expectedStatus) throws Exception {
        return mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().is(expectedStatus))
                .andReturn();
    }

    protected long currentStock(long productId) {
        String sql = "SELECT current_quantity FROM products WHERE id = :id";
        Integer stock = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("id", productId), Integer.class);
        Assertions.assertNotNull(stock, "Producto no encontrado para stock: " + productId);
        return stock;
    }

    protected SaleDbRow fetchSale(long saleId) {
        String sql = """
                SELECT id, client_id, subtotal_amount, discount_total_amount, global_discount_pct, total_amount
                FROM sales
                WHERE id = :saleId
                """;

        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("saleId", saleId), (rs, rowNum) ->
                new SaleDbRow(
                        rs.getLong("id"),
                        rs.getLong("client_id"),
                        rs.getBigDecimal("subtotal_amount"),
                        rs.getBigDecimal("discount_total_amount"),
                        rs.getBigDecimal("global_discount_pct"),
                        rs.getBigDecimal("total_amount")
                )
        );
    }

    protected List<SaleItemDbRow> fetchSaleItems(long saleId) {
        String sql = """
                SELECT product_id, quantity, list_unit_price_snapshot, discount_pct_snapshot, unit_price_snapshot, cost_price_snapshot
                FROM sale_items
                WHERE sale_id = :saleId
                ORDER BY product_id
                """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource("saleId", saleId), (rs, rowNum) ->
                new SaleItemDbRow(
                        rs.getLong("product_id"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("list_unit_price_snapshot"),
                        rs.getBigDecimal("discount_pct_snapshot"),
                        rs.getBigDecimal("unit_price_snapshot"),
                        rs.getBigDecimal("cost_price_snapshot")
                )
        );
    }

    protected long countSaleReasonMovements(long saleId) {
        String sql = """
                SELECT COUNT(*)
                FROM stock_movements sm
                WHERE sm.reason = 'SALE'
                  AND sm.notes = :notes
                """;
        String notes = "Venta #" + saleId;
        Long count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("notes", notes), Long.class);
        return count == null ? 0L : count;
    }

    protected void assertSaleStockDecrementViaMovements(long saleId) {
        String notes = "Venta #" + saleId;
        List<SaleItemDbRow> items = fetchSaleItems(saleId);
        Assertions.assertFalse(items.isEmpty(), "La venta debe tener items");

        long movementCount = countSaleReasonMovements(saleId);
        Assertions.assertEquals(items.size(), movementCount,
                "Debe existir un movimiento SALE por cada item vendido");

        String sql = """
                SELECT COUNT(*)
                FROM sale_items si
                JOIN stock_movements sm
                  ON sm.product_id = si.product_id
                 AND sm.reason = 'SALE'
                 AND sm.notes = :notes
                 AND sm.delta = -si.quantity
                WHERE si.sale_id = :saleId
                """;

        Long matched = jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource()
                        .addValue("notes", notes)
                        .addValue("saleId", saleId),
                Long.class
        );
        Assertions.assertEquals(items.size(), matched == null ? 0L : matched,
                "Los movimientos SALE deben reflejar exactamente el decremento de cada item");
    }

    protected void assertFinancialConsistencyAndSnapshots(long saleId, JsonNode responseBody) {
        SaleDbRow sale = fetchSale(saleId);
        List<SaleItemDbRow> dbItems = fetchSaleItems(saleId);

        Assertions.assertNotNull(sale, "La venta debe persistirse");
        Assertions.assertFalse(dbItems.isEmpty(), "La venta debe tener items persistidos");

        BigDecimal equation = money(sale.subtotalAmount().subtract(sale.discountTotalAmount()));
        assertMoneyEquals(equation, sale.totalAmount(), "subtotal - descuento total debe igualar total");

        for (SaleItemDbRow item : dbItems) {
            Assertions.assertNotNull(item.listUnitPriceSnapshot(), "list_unit_price_snapshot no puede ser null");
            Assertions.assertNotNull(item.discountPctSnapshot(), "discount_pct_snapshot no puede ser null");
            Assertions.assertNotNull(item.unitPriceSnapshot(), "unit_price_snapshot no puede ser null");
            Assertions.assertNotNull(item.costPriceSnapshot(), "cost_price_snapshot no puede ser null");

            BigDecimal expectedLineSubtotal = money(item.unitPriceSnapshot().multiply(BigDecimal.valueOf(item.quantity())));
            Assertions.assertTrue(expectedLineSubtotal.compareTo(BigDecimal.ZERO) >= 0,
                    "subtotal de linea debe ser >= 0");

            BigDecimal expectedNetFromDiscount = money(
                    item.listUnitPriceSnapshot().multiply(
                            BigDecimal.ONE.subtract(item.discountPctSnapshot().divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP))
                    )
            );
            assertWithinOneCent(expectedNetFromDiscount, item.unitPriceSnapshot(),
                    "precio neto debe ser coherente con list+descuento (tolerancia 0.01)");
        }

        assertResponseMatchesPersistence(saleId, responseBody, sale, dbItems);
    }

    protected void assertResponseMatchesPersistence(long saleId, JsonNode responseBody, SaleDbRow sale, List<SaleItemDbRow> dbItems) {
        Assertions.assertEquals(saleId, responseBody.path("id").asLong());
        assertMoneyEquals(sale.subtotalAmount(), asMoney(responseBody, "subtotalAmount"), "subtotal API debe igualar DB");
        assertMoneyEquals(sale.discountTotalAmount(), asMoney(responseBody, "discountTotalAmount"), "descuento API debe igualar DB");
        assertMoneyEquals(sale.globalDiscountPct(), asMoney(responseBody, "globalDiscountPct"), "descuento global API debe igualar DB");
        assertMoneyEquals(sale.totalAmount(), asMoney(responseBody, "totalAmount"), "total API debe igualar DB");

        JsonNode itemsNode = responseBody.path("items");
        Assertions.assertTrue(itemsNode.isArray(), "items en respuesta debe ser array");
        Assertions.assertEquals(dbItems.size(), itemsNode.size(), "cantidad de lineas API/DB debe coincidir");

        Map<Long, SaleItemDbRow> byProduct = new HashMap<>();
        for (SaleItemDbRow row : dbItems) {
            byProduct.put(row.productId(), row);
        }

        List<JsonNode> apiItems = new ArrayList<>();
        itemsNode.forEach(apiItems::add);
        apiItems.sort(Comparator.comparingLong(node -> node.path("productId").asLong()));

        for (JsonNode apiItem : apiItems) {
            long productId = apiItem.path("productId").asLong();
            SaleItemDbRow dbItem = byProduct.get(productId);
            Assertions.assertNotNull(dbItem, "Item API sin correlación en DB para productId=" + productId);

            Assertions.assertEquals(dbItem.quantity(), apiItem.path("quantity").asInt(), "quantity API/DB debe coincidir");
            assertMoneyEquals(dbItem.listUnitPriceSnapshot(), asMoney(apiItem, "listUnitPriceSnapshot"), "list unit API/DB debe coincidir");
            assertMoneyEquals(dbItem.discountPctSnapshot(), asMoney(apiItem, "discountPctSnapshot"), "discount pct API/DB debe coincidir");
            assertMoneyEquals(dbItem.unitPriceSnapshot(), asMoney(apiItem, "unitPriceSnapshot"), "unit net API/DB debe coincidir");

            BigDecimal expectedSubtotal = money(dbItem.unitPriceSnapshot().multiply(BigDecimal.valueOf(dbItem.quantity())));
            assertMoneyEquals(expectedSubtotal, asMoney(apiItem, "subtotal"), "subtotal API debe igualar qty*neto");
        }
    }

    protected JsonNode getFinanzasResumen(int days) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/analitica/finanzas-resumen")
                        .param("days", String.valueOf(days)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    protected JsonNode getAnaliticaMeta(int days) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/analitica/meta")
                        .param("days", String.valueOf(days)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    protected BigDecimal asMoney(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return money(node.get(field).decimalValue());
    }

    protected void assertMoneyEquals(BigDecimal expected, BigDecimal actual, String message) {
        Assertions.assertNotNull(expected, message + " (expected null)");
        Assertions.assertNotNull(actual, message + " (actual null)");
        Assertions.assertEquals(0, money(expected).compareTo(money(actual)), message + " [expected=" + expected + ", actual=" + actual + "]");
    }

    protected void assertWithinOneCent(BigDecimal expected, BigDecimal actual, String message) {
        Assertions.assertNotNull(expected, message + " (expected null)");
        Assertions.assertNotNull(actual, message + " (actual null)");
        BigDecimal diff = money(expected).subtract(money(actual)).abs();
        Assertions.assertTrue(diff.compareTo(new BigDecimal("0.01")) <= 0,
                message + " [expected=" + expected + ", actual=" + actual + ", diff=" + diff + "]");
    }

    protected BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    protected record CreatedSale(long saleId, JsonNode body) {
    }

    protected record SaleDbRow(
            long id,
            long clientId,
            BigDecimal subtotalAmount,
            BigDecimal discountTotalAmount,
            BigDecimal globalDiscountPct,
            BigDecimal totalAmount
    ) {
    }

    protected record SaleItemDbRow(
            long productId,
            int quantity,
            BigDecimal listUnitPriceSnapshot,
            BigDecimal discountPctSnapshot,
            BigDecimal unitPriceSnapshot,
            BigDecimal costPriceSnapshot
    ) {
    }
}
