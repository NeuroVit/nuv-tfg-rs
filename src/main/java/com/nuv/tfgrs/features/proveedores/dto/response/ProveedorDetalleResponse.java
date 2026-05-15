package com.nuv.tfgrs.features.proveedores.dto.response;

import com.nuv.tfgrs.features.analitica.dto.response.ProveedorKpiResponse;
import com.nuv.tfgrs.features.pedidosproveedor.dto.response.PedidoProveedorResumenResponse;
import java.util.List;

public record ProveedorDetalleResponse(
        ProveedorResponse proveedor,
        ProveedorKpiResponse kpis,
        List<ProveedorProductoResumenResponse> productos,
        List<PedidoProveedorResumenResponse> pedidosRecientes
) {
}
