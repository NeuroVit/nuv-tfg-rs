package com.nuv.tfgrs.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("sales-verification")
@WithMockUser(username = "manager", roles = {"MANAGER"})
class VentasApiIntegrationTest extends SalesIntegrationTestBase {

    @Test
    void createSale_withDirectItems_shouldPersistAndUpdateStock() throws Exception {
        Long clientId = insertClient(true);
        long productId = 1L;

        Map<String, Object> payload = salePayload(
                clientId,
                List.of(directItem(productId, 2, null)),
                List.of(),
                null,
                "direct-items"
        );

        CreatedSale created = createSaleExpectCreated(payload);
        assertFinancialConsistencyAndSnapshots(created.saleId(), created.body());

        List<SaleItemDbRow> dbItems = fetchSaleItems(created.saleId());
        assertEquals(1, dbItems.size());
        assertSaleStockDecrementViaMovements(created.saleId());
    }

    @Test
    void createSale_withBundleOnly_shouldApplyDefaultBundleDiscount() throws Exception {
        Long clientId = insertClient(true);

        Map<String, Object> payload = salePayload(
                clientId,
                List.of(),
                List.of(bundleItem(1L, 1, null)),
                null,
                "bundle-default-discount"
        );

        CreatedSale created = createSaleExpectCreated(payload);
        assertFinancialConsistencyAndSnapshots(created.saleId(), created.body());

        List<SaleItemDbRow> dbItems = fetchSaleItems(created.saleId());
        assertFalse(dbItems.isEmpty());
        assertSaleStockDecrementViaMovements(created.saleId());
    }

    @Test
    void createSale_withPerLineDiscount_shouldPersistDiscountSnapshots() throws Exception {
        Long clientId = insertClient(true);
        long productId = 7L;

        Map<String, Object> payload = salePayload(
                clientId,
                List.of(directItem(productId, 3, new BigDecimal("12.50"))),
                List.of(),
                null,
                "line-discount"
        );

        CreatedSale created = createSaleExpectCreated(payload);
        assertFinancialConsistencyAndSnapshots(created.saleId(), created.body());

        SaleItemDbRow row = fetchSaleItems(created.saleId()).get(0);
        assertTrue(row.discountPctSnapshot().compareTo(BigDecimal.ZERO) > 0);
        assertSaleStockDecrementViaMovements(created.saleId());
    }

    @Test
    void createSale_withGlobalDiscount_shouldPersistGlobalAndNetTotals() throws Exception {
        Long clientId = insertClient(true);
        long productId = 9L;

        Map<String, Object> payload = salePayload(
                clientId,
                List.of(directItem(productId, 4, null)),
                List.of(),
                new BigDecimal("15.00"),
                "global-discount"
        );

        CreatedSale created = createSaleExpectCreated(payload);
        assertFinancialConsistencyAndSnapshots(created.saleId(), created.body());

        SaleDbRow sale = fetchSale(created.saleId());
        assertMoneyEquals(new BigDecimal("15.00"), sale.globalDiscountPct(), "descuento global debe persistirse");
        assertSaleStockDecrementViaMovements(created.saleId());
    }

    @Test
    void createSale_withMixedDirectAndBundles_shouldAggregateAndPersistCorrectly() throws Exception {
        Long clientId = insertClient(true);

        Map<String, Object> payload = salePayload(
                clientId,
                List.of(directItem(25L, 2, new BigDecimal("5.00"))),
                List.of(bundleItem(2L, 1, null)),
                new BigDecimal("3.00"),
                "mixed-sale"
        );

        CreatedSale created = createSaleExpectCreated(payload);
        assertFinancialConsistencyAndSnapshots(created.saleId(), created.body());

        List<SaleItemDbRow> dbItems = fetchSaleItems(created.saleId());
        assertFalse(dbItems.isEmpty());
        assertSaleStockDecrementViaMovements(created.saleId());
    }

    @Test
    void createSale_withInsufficientStock_shouldReturnConflict() throws Exception {
        Long clientId = insertClient(true);

        Map<String, Object> payload = salePayload(
                clientId,
                List.of(directItem(17L, 9999, null)),
                List.of(),
                null,
                "insufficient-stock"
        );

        createSaleExpectStatus(payload, 409);
    }

    @Test
    void createSale_withInactiveClient_shouldReturnBadRequest() throws Exception {
        Long inactiveClientId = insertClient(false);

        Map<String, Object> payload = salePayload(
                inactiveClientId,
                List.of(directItem(1L, 1, null)),
                List.of(),
                null,
                "inactive-client"
        );

        createSaleExpectStatus(payload, 400);
    }

    @Test
    void createSale_withDuplicateDirectProduct_shouldReturnBadRequest() throws Exception {
        Long clientId = insertClient(true);

        Map<String, Object> payload = salePayload(
                clientId,
                List.of(
                        directItem(1L, 1, null),
                        directItem(1L, 2, null)
                ),
                List.of(),
                null,
                "duplicate-product"
        );

        createSaleExpectStatus(payload, 400);
    }

    @Test
    void createSale_withDuplicateBundle_shouldReturnBadRequest() throws Exception {
        Long clientId = insertClient(true);

        Map<String, Object> payload = salePayload(
                clientId,
                List.of(),
                List.of(
                        bundleItem(1L, 1, null),
                        bundleItem(1L, 2, null)
                ),
                null,
                "duplicate-bundle"
        );

        createSaleExpectStatus(payload, 400);
    }

    @Test
    void analyticsRegression_afterSale_shouldExposeMarginFieldsAndSnapshotInputs() throws Exception {
        Long clientId = insertClient(true);

        Map<String, Object> payload = salePayload(
                clientId,
                List.of(directItem(7L, 2, new BigDecimal("11.00"))),
                List.of(bundleItem(1L, 1, new BigDecimal("9.00"))),
                new BigDecimal("4.00"),
                "analytics-regression"
        );

        CreatedSale created = createSaleExpectCreated(payload);
        List<SaleItemDbRow> rows = fetchSaleItems(created.saleId());
        assertFalse(rows.isEmpty());
        for (SaleItemDbRow row : rows) {
            assertNotNull(row.listUnitPriceSnapshot());
            assertNotNull(row.discountPctSnapshot());
            assertNotNull(row.unitPriceSnapshot());
            assertNotNull(row.costPriceSnapshot());
        }

        JsonNode finanzas = getFinanzasResumen(30);
        assertTrue(finanzas.hasNonNull("margenReal30d"));
        assertTrue(finanzas.hasNonNull("cogsReal30d"));
        assertTrue(finanzas.hasNonNull("margenBrutoPct"));
        assertTrue(finanzas.hasNonNull("markupPct"));

        assertTrue(finanzas.get("margenReal30d").isNumber());
        assertTrue(finanzas.get("cogsReal30d").isNumber());
        assertTrue(finanzas.get("margenBrutoPct").isNumber());
        assertTrue(finanzas.get("markupPct").isNumber());
    }
}
