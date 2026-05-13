package com.nuv.tfgrs.dto.response;

import java.math.BigDecimal;

public record BundlePerformanceRowResponse(
        Long bundleId,
        String bundleName,
        Integer unitsSold,
        BigDecimal revenue,
        BigDecimal cost,
        BigDecimal profit,
        BigDecimal margin,
        Integer salesCount
) {
}
