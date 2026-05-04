package com.nuv.tfgrs.dto.response;

import com.nuv.tfgrs.entity.MovimientoStock;

import java.time.LocalDateTime;

public record MovimientoStockResponse(
        Long id,
        String productoNombre,
        String productoSku,
        Integer cantidad,
        String tipo,
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
                movimiento.getMovedAt()
        );
    }
}

