package com.nuv.tfgrs.features.productos.dto.response;

import com.nuv.tfgrs.features.productos.entity.Producto;
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
    BigDecimal costoUnitario,
    Long categoriaId,
    String categoriaNombre,
    Long providerId,
    String providerNombre,
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
            null,
            p.getCategory() != null ? p.getCategory().getId() : null,
            p.getCategory() != null ? p.getCategory().getName() : null,
            p.getProvider() != null ? p.getProvider().getId() : null,
            p.getProvider() != null ? p.getProvider().getName() : null,
            p.getVersion(),
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }

    public static ProductoResponse from(Producto p, BigDecimal costoUnitario) {
        return new ProductoResponse(
            p.getId(),
            p.getName(),
            p.getSku(),
            p.getDescription(),
            p.getCurrentQuantity(),
            p.getMinimumStockLevel(),
            p.getUnitPrice(),
            costoUnitario,
            p.getCategory() != null ? p.getCategory().getId() : null,
            p.getCategory() != null ? p.getCategory().getName() : null,
            p.getProvider() != null ? p.getProvider().getId() : null,
            p.getProvider() != null ? p.getProvider().getName() : null,
            p.getVersion(),
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }
}
