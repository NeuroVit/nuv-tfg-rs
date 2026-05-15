package com.nuv.tfgrs.features.ventas.domain;

import com.nuv.tfgrs.features.ventas.dto.request.CrearVentaRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VentaRequestValidatorTest {

    private final VentaRequestValidator validator = new VentaRequestValidator();

    @Test
    void shouldRejectRepeatedProducts() {
        List<CrearVentaRequest.Item> items = List.of(
                new CrearVentaRequest.Item(1L, 1, BigDecimal.ZERO),
                new CrearVentaRequest.Item(1L, 2, BigDecimal.ZERO)
        );

        assertThrows(ResponseStatusException.class, () -> validator.validateNoRepeatedProductsInDirectItems(items));
    }

    @Test
    void shouldRejectRepeatedBundles() {
        List<CrearVentaRequest.BundleItem> bundles = List.of(
                new CrearVentaRequest.BundleItem(1L, 1, BigDecimal.ZERO),
                new CrearVentaRequest.BundleItem(1L, 2, BigDecimal.ZERO)
        );

        assertThrows(ResponseStatusException.class, () -> validator.validateNoRepeatedBundles(bundles));
    }

    @Test
    void shouldAllowUniqueIds() {
        List<CrearVentaRequest.Item> items = List.of(
                new CrearVentaRequest.Item(1L, 1, BigDecimal.ZERO),
                new CrearVentaRequest.Item(2L, 2, BigDecimal.ZERO)
        );
        List<CrearVentaRequest.BundleItem> bundles = List.of(
                new CrearVentaRequest.BundleItem(1L, 1, BigDecimal.ZERO),
                new CrearVentaRequest.BundleItem(2L, 1, BigDecimal.ZERO)
        );

        assertDoesNotThrow(() -> validator.validateNoRepeatedProductsInDirectItems(items));
        assertDoesNotThrow(() -> validator.validateNoRepeatedBundles(bundles));
    }
}
