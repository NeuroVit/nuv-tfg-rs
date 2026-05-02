package com.nuv.tfgrs.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record BundleUpsertRequest(
        @NotBlank
        @Size(max = 200)
        String nombre,

        @Size(max = 500)
        String descripcion,

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("100.0")
        BigDecimal descuentoPct,

        @NotNull
        Boolean activo,

        @NotEmpty
        List<@Valid Item> items
) {
    public record Item(
            @NotNull
            Long productoId,

            @NotNull
            @Min(1)
            Integer cantidad
    ) {
    }
}
