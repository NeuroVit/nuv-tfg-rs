package com.nuv.tfgrs.features.analitica.dto.response;

import java.math.BigDecimal;

public record TopProductoVelocidadRowResponse(
        Long productId,
        String sku,
        String productName,
        String categoryName,
        Integer unitsSold,
        BigDecimal revenue
) {
}
