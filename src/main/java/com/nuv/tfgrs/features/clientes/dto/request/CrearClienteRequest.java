package com.nuv.tfgrs.features.clientes.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearClienteRequest(
        @NotBlank
        @Size(min = 2, max = 200)
        @Pattern(regexp = "^(?!.*\\d).+$", message = "El nombre no puede contener numeros")
        String nombre,
        @Email String email,
        @Pattern(regexp = "^$|^\\d{9}$", message = "El telefono debe tener exactamente 9 numeros")
        String telefono,
        @Size(max = 500) String notas
) {
}
