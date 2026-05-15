package com.nuv.tfgrs.features.ventas.service;

import com.nuv.tfgrs.features.ventas.dto.response.VentaResponse;
import com.nuv.tfgrs.features.clientes.entity.Cliente;

public interface VentaEmailService {
    void enviarComprobanteVenta(VentaResponse venta, Cliente cliente);
}

