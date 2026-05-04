package com.nuv.tfgrs.dto.response;

import java.math.BigDecimal;

public record InventorySummaryRowResponse(
        Long productId,
        String sku,
        String productName,
        String categoryName,
        Integer currentQuantity,
        Integer minimumStockLevel,
        BigDecimal unitPrice,
        BigDecimal stockValue,
        String stockStatus
) {
}
