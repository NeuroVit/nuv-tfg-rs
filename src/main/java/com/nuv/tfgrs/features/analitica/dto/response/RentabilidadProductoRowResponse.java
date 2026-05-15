package com.nuv.tfgrs.features.analitica.dto.response;

import java.math.BigDecimal;

public record RentabilidadProductoRowResponse(
        Long productId,
        String sku,
        String productName,
        String categoryName,
        Integer unidadesVendidas,
        BigDecimal revenue,
        BigDecimal cogs,
        BigDecimal margen,
        BigDecimal margenPct
) {
}
