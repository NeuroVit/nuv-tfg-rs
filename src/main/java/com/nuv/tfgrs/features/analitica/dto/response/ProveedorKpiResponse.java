package com.nuv.tfgrs.features.analitica.dto.response;

public record ProveedorKpiResponse(
        long totalPedidos,
        long pedidosActivos,
        long pedidosRecibidos,
        double fillRate,
        double onTimeRate
) {
}
