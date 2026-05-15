package com.nuv.tfgrs.features.ventas.domain;

import com.nuv.tfgrs.features.productos.entity.Producto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class VentaPricingPolicy {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public BigDecimal applyDiscount(BigDecimal amount, BigDecimal discountPct) {
        BigDecimal normalizedAmount = amount == null ? BigDecimal.ZERO : amount;
        BigDecimal pct = normalizePct(discountPct);
        BigDecimal factor = BigDecimal.ONE.subtract(pct.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP));
        return normalizedAmount.multiply(factor);
    }

    public BigDecimal normalizePct(BigDecimal pct) {
        if (pct == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (pct.compareTo(BigDecimal.ZERO) < 0 || pct.compareTo(ONE_HUNDRED) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El descuento debe estar entre 0 y 100");
        }
        return pct.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal safePercent(BigDecimal value, BigDecimal base) {
        if (base == null || base.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.multiply(ONE_HUNDRED).divide(base, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal resolveUnitCost(Producto product) {
        if (product.getAcquisitionPrice() != null) {
            return product.getAcquisitionPrice();
        }
        return product.getUnitPrice();
    }
}
