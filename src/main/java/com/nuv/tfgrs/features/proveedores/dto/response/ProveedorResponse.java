package com.nuv.tfgrs.features.proveedores.dto.response;

import com.nuv.tfgrs.features.proveedores.entity.Proveedor;

public record ProveedorResponse(
        Long id,
        String nombre,
        String email,
        String ubicacionAlmacen,
        Integer diasEntrega,
        boolean activo,
        long cantidadProductos
) {
    public static ProveedorResponse from(Proveedor proveedor, long cantidadProductos) {
        return new ProveedorResponse(
                proveedor.getId(),
                proveedor.getName(),
                proveedor.getEmail(),
                proveedor.getWarehouseLocation(),
                proveedor.getLeadTimeDays(),
                Boolean.TRUE.equals(proveedor.getActive()),
                cantidadProductos
        );
    }
}
