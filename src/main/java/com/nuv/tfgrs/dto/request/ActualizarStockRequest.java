package com.nuv.tfgrs.dto.request;

import com.nuv.tfgrs.entity.MovimientoStock.Reason;
import jakarta.validation.constraints.NotNull;

public record ActualizarStockRequest(
    @NotNull Integer delta,
    @NotNull Reason razon,
    String notas
) {}