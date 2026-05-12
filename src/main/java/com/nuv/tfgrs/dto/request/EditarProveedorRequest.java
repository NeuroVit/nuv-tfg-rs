package com.nuv.tfgrs.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EditarProveedorRequest(
        @NotBlank
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotBlank
        @Size(max = 500)
        String ubicacionAlmacen,

        @NotNull
        @Min(value = 1, message = "Días de entrega debe ser mayor o igual a 1")
        Integer diasEntrega
) {
}
