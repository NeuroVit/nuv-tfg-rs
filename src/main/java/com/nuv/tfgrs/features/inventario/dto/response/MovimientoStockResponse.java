package com.nuv.tfgrs.features.inventario.dto.response;

import com.nuv.tfgrs.features.inventario.entity.MovimientoStock;

import java.time.LocalDateTime;

public record MovimientoStockResponse(
        Long id,
        String productoNombre,
        String productoSku,
        Integer cantidad,
        String tipo,
        String razon,
        String notas,
        LocalDateTime fecha
) {
    public static MovimientoStockResponse from(MovimientoStock movimiento) {
        int delta = movimiento.getDelta() == null ? 0 : movimiento.getDelta();
        String tipo = delta >= 0 ? "ENTRADA" : "SALIDA";
        return new MovimientoStockResponse(
                movimiento.getId(),
                movimiento.getProduct().getName(),
                movimiento.getProduct().getSku(),
                Math.abs(delta),
                tipo,
                movimiento.getReason() != null ? movimiento.getReason().name() : "ADJUSTMENT",
                movimiento.getNotes(),
                movimiento.getMovedAt()
        );
    }
}
