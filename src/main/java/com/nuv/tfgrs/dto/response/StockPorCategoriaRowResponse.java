package com.nuv.tfgrs.dto.response;

import java.math.BigDecimal;

public record StockPorCategoriaRowResponse(
        Long categoryId,
        String categoryName,
        Long products,
        Long quantityTotal,
        BigDecimal stockValueTotal
) {
}
