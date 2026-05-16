package com.nuv.tfgrs.features.productos.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record EditarProductoRequest(
        String nombre,
        String sku,
        String descripcion,
        @Positive BigDecimal precioUnitario,
        @Positive BigDecimal costoUnitario,
        @PositiveOrZero Integer cantidadActual,
        @PositiveOrZero Integer nivelMinimoStock,
        @PositiveOrZero Integer nivelMaximoStock,
        Long categoriaId,
        Long providerId
) {}
