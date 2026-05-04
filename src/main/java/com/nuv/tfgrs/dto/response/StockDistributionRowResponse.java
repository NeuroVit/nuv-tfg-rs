package com.nuv.tfgrs.dto.response;

import java.math.BigDecimal;

public record StockDistributionRowResponse(
        String stockStatus,
        Long products,
        Long quantityTotal,
        BigDecimal stockValueTotal
) {
}
