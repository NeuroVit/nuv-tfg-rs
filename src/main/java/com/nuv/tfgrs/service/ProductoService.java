package com.nuv.tfgrs.service;

import com.nuv.tfgrs.dto.request.ActualizarStockRequest;
import com.nuv.tfgrs.dto.request.CrearProductoRequest;
import com.nuv.tfgrs.dto.request.EditarProductoRequest;
import com.nuv.tfgrs.dto.response.ProductoResponse;
import java.util.List;

public interface ProductoService {
    List<ProductoResponse> listarTodos();
    List<ProductoResponse> listarBajoStock();
    ProductoResponse buscarPorId(Long id);
    ProductoResponse crear(CrearProductoRequest request);
    ProductoResponse actualizar(Long id, EditarProductoRequest request);
    ProductoResponse actualizarStock(Long id, ActualizarStockRequest request);
    void eliminar(Long id);
}
