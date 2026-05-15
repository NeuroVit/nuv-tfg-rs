package com.nuv.tfgrs.features.organizaciones.dto.request;

import jakarta.validation.constraints.NotNull;

public record JoinOrganizationRequest(
        @NotNull(message = "La organización es obligatoria")
        Long organizationId
) {
}
