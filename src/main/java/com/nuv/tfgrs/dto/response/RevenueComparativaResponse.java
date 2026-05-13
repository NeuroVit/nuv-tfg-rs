package com.nuv.tfgrs.dto.response;

import java.util.List;

public record RevenueComparativaResponse(
        String currentLabel,
        String previousLabel,
        String granularity,
        List<RevenueComparativaBucketResponse> buckets
) {
}
