package com.nuv.tfgrs.dto.response;

import java.util.List;

public record ProveedorDetalleResponse(
        ProveedorResponse proveedor,
        ProveedorKpiResponse kpis,
        List<ProveedorProductoResumenResponse> productos,
        List<PedidoProveedorResumenResponse> pedidosRecientes
) {
}
