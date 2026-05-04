package com.nuv.tfgrs.dto.response;

import com.nuv.tfgrs.entity.Proveedor;

public record ProveedorResponse(
        Long id,
        String nombre,
        String ubicacionAlmacen,
        Integer diasEntrega,
        boolean activo,
        long cantidadProductos
) {
    public static ProveedorResponse from(Proveedor proveedor, long cantidadProductos) {
        return new ProveedorResponse(
                proveedor.getId(),
                proveedor.getName(),
                proveedor.getWarehouseLocation(),
                proveedor.getLeadTimeDays(),
                Boolean.TRUE.equals(proveedor.getActive()),
                cantidadProductos
        );
    }
}
