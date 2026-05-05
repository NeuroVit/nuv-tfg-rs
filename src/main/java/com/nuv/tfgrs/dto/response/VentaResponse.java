package com.nuv.tfgrs.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VentaResponse(
        Long id,
        Long clientId,
        String clientName,
        LocalDateTime soldAt,
        String notas,
        BigDecimal subtotalAmount,
        BigDecimal discountTotalAmount,
        BigDecimal globalDiscountPct,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        List<VentaItemResponse> items
) {
}
