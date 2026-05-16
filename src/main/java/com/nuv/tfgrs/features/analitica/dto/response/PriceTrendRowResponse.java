package com.nuv.tfgrs.features.analitica.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceTrendRowResponse(
        Long historyId,
        Long productId,
        String sku,
        String productName,
        String categoryName,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        BigDecimal priceDelta,
        BigDecimal pctChange,
        LocalDateTime changedAt,
        BigDecimal currentPrice
) {
}
