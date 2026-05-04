package com.nuv.tfgrs.dto.response;

import com.nuv.tfgrs.entity.AlertaStock;

import java.time.LocalDateTime;

public record AlertaStockResponse(
        Long id,
        Long productoId,
        String productoNombre,
        String productoSku,
        String tipo,
        String mensaje,
        boolean resuelta,
        LocalDateTime creadaEn,
        LocalDateTime resueltaEn
) {
    public static AlertaStockResponse from(AlertaStock alert) {
        return new AlertaStockResponse(
                alert.getId(),
                alert.getProduct().getId(),
                alert.getProduct().getName(),
                alert.getProduct().getSku(),
                alert.getAlertType().name(),
                alert.getMessage(),
                Boolean.TRUE.equals(alert.getResolved()),
                alert.getCreatedAt(),
                alert.getResolvedAt()
        );
    }
}
