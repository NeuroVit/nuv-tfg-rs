package com.nuv.tfgrs.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesVelocityRowResponse(
        Long productId,
        String sku,
        String productName,
        String categoryName,
        LocalDate movementDate,
        Integer unitsSold,
        BigDecimal revenue
) {
}
