package com.nuv.tfgrs.features.inventario.dto.request;

import com.nuv.tfgrs.features.inventario.entity.MovimientoStock.Reason;
import jakarta.validation.constraints.NotNull;

public record ActualizarStockRequest(
    @NotNull Integer delta,
    @NotNull Reason razon,
    String notas
) {}