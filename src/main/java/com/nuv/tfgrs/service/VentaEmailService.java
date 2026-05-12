package com.nuv.tfgrs.service;

import com.nuv.tfgrs.dto.response.VentaResponse;
import com.nuv.tfgrs.entity.Cliente;

public interface VentaEmailService {
    void enviarComprobanteVenta(VentaResponse venta, Cliente cliente);
}

