package com.nuv.tfgrs.service;

import com.nuv.tfgrs.dto.request.CrearPedidoProveedorRequest;
import com.nuv.tfgrs.dto.response.PedidoProveedorResponse;

import java.util.List;

public interface PedidoProveedorService {
    List<PedidoProveedorResponse> listarPorProveedor(Long providerId);
    PedidoProveedorResponse crearBorrador(Long providerId, CrearPedidoProveedorRequest request);
    PedidoProveedorResponse enviar(Long orderId);
    PedidoProveedorResponse recibir(Long orderId);
    PedidoProveedorResponse cancelar(Long orderId);
}
