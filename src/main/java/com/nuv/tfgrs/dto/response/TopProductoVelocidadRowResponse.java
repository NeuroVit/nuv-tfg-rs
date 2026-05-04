package com.nuv.tfgrs.dto.response;

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
