package com.nuv.tfgrs.features.analitica.dto.response;

import java.math.BigDecimal;

public record StockDistributionRowResponse(
        String stockStatus,
        Long products,
        Long quantityTotal,
        BigDecimal stockValueTotal
) {
}
