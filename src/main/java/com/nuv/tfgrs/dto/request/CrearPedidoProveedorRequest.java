package com.nuv.tfgrs.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CrearPedidoProveedorRequest(
        @NotNull
        LocalDate fechaLlegadaDeseada,

        @Size(max = 500)
        String notas,

        @NotEmpty
        List<@Valid Item> items
) {
    public record Item(
            @NotNull
            Long productoId,

            @NotNull
            @Min(1)
            Integer cantidad
    ) {
    }
}
