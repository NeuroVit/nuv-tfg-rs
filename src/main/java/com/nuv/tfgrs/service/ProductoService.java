package com.nuv.tfgrs.service;

import com.nuv.tfgrs.dto.request.ActualizarStockRequest;
import com.nuv.tfgrs.dto.request.ProductoRequest;
import com.nuv.tfgrs.dto.response.ProductoResponse;
import java.util.List;

public interface ProductoService {
    List<ProductoResponse> listarTodos();
    List<ProductoResponse> listarBajoStock();
    ProductoResponse buscarPorId(Long id);
    ProductoResponse crear(ProductoRequest request);
    ProductoResponse actualizar(Long id, ProductoRequest request);
    ProductoResponse actualizarStock(Long id, ActualizarStockRequest request);
    void eliminar(Long id);
}