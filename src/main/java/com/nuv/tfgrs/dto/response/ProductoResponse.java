package com.nuv.tfgrs.dto.response;

import com.nuv.tfgrs.entity.Producto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoResponse(
    Long id,
    String nombre,
    String sku,
    String descripcion,
    Integer cantidadActual,
    Integer nivelMinimoStock,
    BigDecimal precioUnitario,
    Long categoriaId,
    String categoriaNombre,
    Long version,
    LocalDateTime creadoEn,
    LocalDateTime actualizadoEn
) {
    public static ProductoResponse from(Producto p) {
        return new ProductoResponse(
            p.getId(),
            p.getName(),
            p.getSku(),
            p.getDescription(),
            p.getCurrentQuantity(),
            p.getMinimumStockLevel(),
            p.getUnitPrice(),
            p.getCategory() != null ? p.getCategory().getId() : null,
            p.getCategory() != null ? p.getCategory().getName() : null,
            p.getVersion(),
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }
}
