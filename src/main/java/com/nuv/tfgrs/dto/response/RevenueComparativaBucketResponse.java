package com.nuv.tfgrs.dto.response;

import java.math.BigDecimal;

public record RevenueComparativaBucketResponse(
        String label,
        BigDecimal currentRevenue,
        BigDecimal previousRevenue
) {
}
