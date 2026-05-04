package com.nuv.tfgrs.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CrearVentaRequest(
        @NotNull Long clientId,
        @Size(max = 500) String notas,
        @Valid @NotEmpty List<Item> items
) {
    public record Item(
            @NotNull Long productId,
            @Positive Integer quantity
    ) {
    }
}
