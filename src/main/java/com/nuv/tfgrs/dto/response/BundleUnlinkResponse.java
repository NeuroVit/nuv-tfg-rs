package com.nuv.tfgrs.dto.response;

import java.util.List;

public record BundleUnlinkResponse(
        Long productId,
        int unlinkedItems,
        List<String> bundles
) {
}
