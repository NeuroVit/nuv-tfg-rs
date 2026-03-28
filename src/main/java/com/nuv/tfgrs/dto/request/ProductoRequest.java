package com.nuv.tfgrs.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductoRequest(
    @NotBlank String nombre,
    @NotBlank String sku,
    String descripcion,
    @NotNull @Min(0) Integer cantidadActual,
    @NotNull @Min(0) Integer nivelMinimoStock,
    @NotNull @DecimalMin("0.0") BigDecimal precioUnitario,
    @NotNull Long categoriaId
) {}