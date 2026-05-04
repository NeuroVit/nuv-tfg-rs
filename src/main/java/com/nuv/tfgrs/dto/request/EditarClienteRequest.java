package com.nuv.tfgrs.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditarClienteRequest(
        @NotBlank @Size(min = 2, max = 200) String nombre,
        @Email String email,
        @Size(max = 40) String telefono,
        @Size(max = 500) String notas
) {
}
