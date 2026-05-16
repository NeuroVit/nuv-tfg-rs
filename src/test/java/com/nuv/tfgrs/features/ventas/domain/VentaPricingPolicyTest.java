package com.nuv.tfgrs.features.ventas.domain;

import com.nuv.tfgrs.features.productos.entity.Producto;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VentaPricingPolicyTest {

    private final VentaPricingPolicy policy = new VentaPricingPolicy();

    @Test
    void shouldApplyDiscount() {
        BigDecimal result = policy.applyDiscount(new BigDecimal("100"), new BigDecimal("15"));
        assertEquals(new BigDecimal("85.000000"), result.setScale(6));
    }

    @Test
    void shouldNormalizeNullPctToZero() {
        assertEquals(new BigDecimal("0.00"), policy.normalizePct(null));
    }

    @Test
    void shouldRejectPctOverHundred() {
        assertThrows(ResponseStatusException.class, () -> policy.normalizePct(new BigDecimal("120")));
    }

    @Test
    void shouldResolveUnitCostUsingAcquisitionPriceWhenAvailable() {
        Producto p = new Producto();
        p.setUnitPrice(new BigDecimal("20"));
        p.setAcquisitionPrice(new BigDecimal("10"));

        assertEquals(new BigDecimal("10"), policy.resolveUnitCost(p));
    }
}
