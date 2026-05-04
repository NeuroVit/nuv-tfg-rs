package com.nuv.tfgrs.dto.response;

public record ProveedorKpiResponse(
        long totalPedidos,
        long pedidosActivos,
        long pedidosRecibidos,
        double fillRate,
        double onTimeRate
) {
}
