package com.nuv.tfgrs.features.bundles.dto.response;

import java.math.BigDecimal;

public record BundleCandidateRowResponse(
        Long productAId,
        String productASku,
        String productAName,
        BigDecimal productAPrice,
        Long productBId,
        String productBSku,
        String productBName,
        BigDecimal productBPrice,
        Long coOccurrenceCount,
        BigDecimal combinedPrice
) {
}
