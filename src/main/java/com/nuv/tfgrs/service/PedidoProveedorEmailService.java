package com.nuv.tfgrs.service;

import com.nuv.tfgrs.dto.response.PedidoProveedorResponse;
import com.nuv.tfgrs.entity.Proveedor;

public interface PedidoProveedorEmailService {
    void enviarPedidoAProveedor(PedidoProveedorResponse pedido, Proveedor proveedor);
}

