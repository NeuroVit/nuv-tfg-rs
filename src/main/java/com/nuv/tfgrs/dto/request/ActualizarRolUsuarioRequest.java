package com.nuv.tfgrs.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ActualizarRolUsuarioRequest(
        @NotBlank String role,
        String reason
) {
}
