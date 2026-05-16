package com.nuv.tfgrs.features.bundles.dto.response;

public record BundleItemResponse(
        Long productoId,
        String productoNombre,
        String sku,
        int cantidad
) {
}
