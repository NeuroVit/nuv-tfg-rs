package com.nuv.tfgrs.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("sales-smoke")
@Tag("sales-verification")
@WithMockUser(username = "admin", roles = {"ADMIN"})
class SalesMathSmokeTest extends SalesIntegrationTestBase {

    @Test
    void smoke_mixedSaleAndAnalytics_shouldStayBusinessConsistent() throws Exception {
        Long clientId = insertClient(true);

        Map<String, Object> payload = salePayload(
                clientId,
                List.of(directItem(9L, 3, new BigDecimal("6.00"))),
                List.of(bundleItem(1L, 1, null)),
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

        Map<String, Object> payload = salePayload(
                clientId,
                List.of(directItem(17L, 99999, null)),
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
