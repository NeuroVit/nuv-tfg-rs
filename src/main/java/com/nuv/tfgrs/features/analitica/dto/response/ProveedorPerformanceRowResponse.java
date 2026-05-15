package com.nuv.tfgrs.features.analitica.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProveedorPerformanceRowResponse(
        Long providerId,
        String providerName,
        Integer orderCount,
        BigDecimal totalSpend,
        BigDecimal avgLeadTimeDays,
        BigDecimal onTimePct,
        LocalDateTime lastOrderAt
) {
}
