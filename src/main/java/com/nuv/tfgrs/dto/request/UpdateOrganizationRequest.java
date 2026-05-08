package com.nuv.tfgrs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @NotBlank(message = "El nombre de la organización es obligatorio")
        @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
        String name
) {
}
