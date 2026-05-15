package com.nuv.tfgrs.features.bundles.dto.response;

import java.util.List;

public record BundleUnlinkResponse(
        Long productId,
        int unlinkedItems,
        List<String> bundles
) {
}
