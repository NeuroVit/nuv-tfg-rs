package com.nuv.tfgrs.features.analitica.dto.response;

import java.math.BigDecimal;

public record StockPorCategoriaRowResponse(
        Long categoryId,
        String categoryName,
        Long products,
        Long quantityTotal,
        BigDecimal stockValueTotal
) {
}
