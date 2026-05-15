package com.nuv.tfgrs.features.analitica.dto.response;

import java.math.BigDecimal;

public record RevenueComparativaBucketResponse(
        String label,
        BigDecimal currentRevenue,
        BigDecimal previousRevenue
) {
}
