package com.nuv.tfgrs.dto.response;

public record BundleItemResponse(
        Long productoId,
        String productoNombre,
        String sku,
        int cantidad
) {
}
