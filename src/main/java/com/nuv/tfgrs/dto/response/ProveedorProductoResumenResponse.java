package com.nuv.tfgrs.dto.response;

public record ProveedorProductoResumenResponse(
        Long id,
        String nombre,
        String sku,
        Integer cantidadActual,
        Integer nivelMinimoStock,
        boolean stockBajo
) {
}
