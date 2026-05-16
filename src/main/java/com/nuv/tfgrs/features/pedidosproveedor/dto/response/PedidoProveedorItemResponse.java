package com.nuv.tfgrs.features.pedidosproveedor.dto.response;

import java.math.BigDecimal;

public record PedidoProveedorItemResponse(
        Long productoId,
        String productoNombre,
        String sku,
        int cantidad,
        BigDecimal precioUnitarioSnapshot
) {
}
