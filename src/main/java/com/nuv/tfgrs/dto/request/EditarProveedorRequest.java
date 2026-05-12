package com.nuv.tfgrs.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;

public record EditarProveedorRequest(
        @NotBlank
        @Size(max = 200)
        String nombre,

        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(max = 500)
        String ubicacionAlmacen,

        @NotNull
        @Min(0)
        Integer diasEntrega
) {
}
