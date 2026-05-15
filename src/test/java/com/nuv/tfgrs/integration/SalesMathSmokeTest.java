package com.nuv.tfgrs.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("sales-smoke")
@Tag("sales-verification")
@WithMockUser(username = "nova_admin", roles = {"ADMIN"})
class SalesMathSmokeTest extends SalesIntegrationTestBase {

    @Test
    void smoke_mixedSaleAndAnalytics_shouldStayBusinessConsistent() throws Exception {
        Long clientId = insertClient(true);
        long bundleId = bundleIdWithMinComponentStock(1);
        long directProductId = productIdWithMinStockExcluding(3, productIdsInBundle(bundleId));

        Map<String, Object> payload = salePayload(
                clientId,
                List.of(directItem(directProductId, 3, new BigDecimal("6.00"))),
                List.of(bundleItem(bundleId, 1, null)),
                new BigDecimal("2.00"),
                "smoke-mixed-sale"
        );

        CreatedSale created = createSaleExpectCreated(payload);
        assertFinancialConsistencyAndSnapshots(created.saleId(), created.body());
        assertSaleStockDecrementViaMovements(created.saleId());

        JsonNode finanzas = getFinanzasResumen(30);
        assertTrue(finanzas.hasNonNull("margenReal30d"));
        assertTrue(finanzas.hasNonNull("cogsReal30d"));
        assertTrue(finanzas.get("margenReal30d").isNumber());
        assertTrue(finanzas.get("cogsReal30d").isNumber());
    }

    @Test
    void smoke_invalidStock_shouldReturnConflict() throws Exception {
        Long clientId = insertClient(true);
        long productId = productIdWithMinStock(1);
        int excessiveQuantity = Math.toIntExact(currentStock(productId) + 1L);

        Map<String, Object> payload = salePayload(
                clientId,
                List.of(directItem(productId, excessiveQuantity, null)),
                List.of(),
                null,
                "smoke-invalid-stock"
        );

        createSaleExpectStatus(payload, 409);
    }

    @Test
    void smoke_analyticsMeta_shouldExposeSourceSeedAndTimestamp() throws Exception {
        JsonNode meta = getAnaliticaMeta(30);

        assertTrue(meta.hasNonNull("source"));
        assertTrue(meta.has("seedEnabled"));
        assertTrue(meta.hasNonNull("generatedAt"));
        assertTrue(meta.has("windowDays"));
        assertTrue(meta.get("seedEnabled").isBoolean());
        assertTrue(meta.get("windowDays").isInt());
        String source = meta.get("source").asText();
        assertTrue(
                source.equals("API_REAL")
                        || source.equals("API_REAL_LOCAL_SEEDED")
                        || source.equals("API_REAL_SEED_PENDING")
        );
    }
}
