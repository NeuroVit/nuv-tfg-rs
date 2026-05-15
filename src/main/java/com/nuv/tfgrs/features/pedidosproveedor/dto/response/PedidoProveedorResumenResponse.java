package com.nuv.tfgrs.features.pedidosproveedor.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PedidoProveedorResumenResponse(
        Long id,
        String status,
        LocalDate fechaLlegadaDeseada,
        LocalDate fechaPedidoRecomendada,
        LocalDateTime enviadoEn,
        LocalDateTime recibidoEn,
        LocalDateTime creadoEn,
        int totalItems,
        int totalUnidades,
        double importeEstimado
) {
}
