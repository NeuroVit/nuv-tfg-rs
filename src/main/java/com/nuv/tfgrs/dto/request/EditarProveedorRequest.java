package com.nuv.tfgrs.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record EditarProveedorRequest(
        @NotBlank
        @Size(max = 200)
        @Pattern(regexp = "^(?!.*\\d).+$", message = "El nombre no puede contener numeros")
        String nombre,

        @NotBlank
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
