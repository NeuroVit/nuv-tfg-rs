package com.nuv.tfgrs.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record EditarProductoRequest(
        String nombre,
        String sku,
        String descripcion,
        @Positive BigDecimal precioUnitario,
        @PositiveOrZero Integer cantidadActual,
        @PositiveOrZero Integer nivelMinimoStock,
        Long categoriaId,
        Long providerId
) {}
