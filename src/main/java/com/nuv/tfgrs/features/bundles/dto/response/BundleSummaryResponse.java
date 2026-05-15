package com.nuv.tfgrs.features.bundles.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BundleSummaryResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal descuentoPct,
        boolean activo,
        int cantidadProductos,
        LocalDateTime actualizadoEn
) {
}
