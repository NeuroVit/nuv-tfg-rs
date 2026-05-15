package com.nuv.tfgrs.features.pedidosproveedor.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoProveedorResponse(
        Long id,
        Long providerId,
        String providerNombre,
        String status,
        LocalDate fechaLlegadaDeseada,
        LocalDate fechaPedidoRecomendada,
        LocalDateTime enviadoEn,
        LocalDateTime recibidoEn,
        String notas,
        LocalDateTime creadoEn,
        List<PedidoProveedorItemResponse> items
) {
}
