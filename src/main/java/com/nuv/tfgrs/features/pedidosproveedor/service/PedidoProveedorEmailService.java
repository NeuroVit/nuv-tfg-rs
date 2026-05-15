package com.nuv.tfgrs.features.pedidosproveedor.service;

import com.nuv.tfgrs.features.pedidosproveedor.dto.response.PedidoProveedorResponse;
import com.nuv.tfgrs.features.proveedores.entity.Proveedor;

public interface PedidoProveedorEmailService {
    void enviarPedidoAProveedor(PedidoProveedorResponse pedido, Proveedor proveedor);
}

