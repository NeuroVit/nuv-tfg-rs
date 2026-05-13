package com.nuv.tfgrs.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("sales-verification")
@WithMockUser(username = "nova_manager", roles = {"MANAGER"})
class AnaliticaApiIntegrationTest extends SalesIntegrationTestBase {

    @Test
    void bundlesPerformance_shouldReturnRowsWhenSaleHasBundleAttribution() throws Exception {
        Long clientId = insertClient(true);
        long bundleId = bundleIdWithMinComponentStock(1);
        Map<String, Object> payload = salePayload(
                clientId,
                List.of(),
                List.of(bundleItem(bundleId, 1, new BigDecimal("5.00"))),
                null,
                "analytics-bundle-performance"
        );
        createSaleExpectCreated(payload);

        JsonNode body = getAnaliticaArray("/api/analitica/bundles-performance", 3650, 20);
        assertTrue(body.isArray());
        assertTrue(body.size() > 0);
        JsonNode row = body.get(0);
        assertTrue(row.has("bundleId"));
        assertTrue(row.has("bundleName"));
        assertTrue(row.has("revenue"));
        assertTrue(row.has("profit"));
    }

    @Test
    void clientesRentabilidad_shouldReturnRows() throws Exception {
        Long clientId = insertClient(true);
        long productId = productIdWithMinStock(1);
        Map<String, Object> payload = salePayload(
                clientId,
                List.of(directItem(productId, 1, null)),
                List.of(),
                null,
                "analytics-cliente-rentabilidad"
        );
        createSaleExpectCreated(payload);

        JsonNode body = getAnaliticaArray("/api/analitica/clientes-rentabilidad", 3650, 20);
        assertTrue(body.isArray());
        assertTrue(body.size() > 0);
        JsonNode row = body.get(0);
        assertTrue(row.has("clientId"));
        assertTrue(row.has("clientName"));
        assertTrue(row.has("avgTicket"));
        assertTrue(row.has("lastSaleAt"));
    }

    @Test
    void proveedoresPerformance_shouldReturnArrayShape() throws Exception {
        JsonNode body = getAnaliticaArray("/api/analitica/proveedores-performance", 3650, 20);
        assertTrue(body.isArray());
        if (body.size() > 0) {
            JsonNode row = body.get(0);
            assertTrue(row.has("providerId"));
            assertTrue(row.has("providerName"));
            assertTrue(row.has("totalSpend"));
            assertTrue(row.has("onTimePct"));
        }
    }

    @Test
    void alertasSerie_shouldReturnArrayShape() throws Exception {
        JsonNode body = getAnaliticaArray("/api/analitica/alertas-serie", 3650, null);
        assertTrue(body.isArray());
        if (body.size() > 0) {
            JsonNode row = body.get(0);
            assertTrue(row.has("bucketDate"));
            assertTrue(row.has("alertType"));
            assertTrue(row.has("createdCount"));
            assertTrue(row.has("resolvedCount"));
        }
    }

    private JsonNode getAnaliticaArray(String path, int days, Integer limit) throws Exception {
        var requestBuilder = get(path)
                .param("days", String.valueOf(days))
                .accept(MediaType.APPLICATION_JSON);
        if (limit != null) {
            requestBuilder = requestBuilder.param("limit", String.valueOf(limit));
        }

        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }
}
