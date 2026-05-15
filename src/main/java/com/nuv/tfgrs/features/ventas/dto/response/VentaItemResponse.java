package com.nuv.tfgrs.features.ventas.dto.response;

import java.math.BigDecimal;

public record VentaItemResponse(
        Long productId,
        Long bundleId,
        String bundleName,
        String productoNombre,
        String sku,
        Integer quantity,
        BigDecimal listUnitPriceSnapshot,
        BigDecimal discountPctSnapshot,
        BigDecimal unitPriceSnapshot,
        BigDecimal subtotal
) {
}
