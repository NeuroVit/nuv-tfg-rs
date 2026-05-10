package com.nuv.tfgrs.dto.request;

import jakarta.validation.constraints.NotNull;

public record JoinOrganizationRequest(
        @NotNull(message = "La organización es obligatoria")
        Long organizationId
) {
}
