package com.nuv.tfgrs.dto.response;

import java.math.BigDecimal;

public record VentaItemResponse(
        Long productId,
        String productoNombre,
        String sku,
        Integer quantity,
        BigDecimal unitPriceSnapshot,
        BigDecimal subtotal
) {
}
