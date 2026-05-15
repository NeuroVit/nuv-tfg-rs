package com.nuv.tfgrs.features.productos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CrearProductoRequest(
        @NotBlank String nombre,
        @NotBlank String sku,
        String descripcion,
        @NotNull @Positive BigDecimal precioUnitario,
        @Positive BigDecimal costoUnitario,
        @NotNull @PositiveOrZero Integer cantidadActual,
        @NotNull @PositiveOrZero Integer nivelMinimoStock,
        @NotNull Long categoriaId,
        @NotNull Long providerId
) {}
