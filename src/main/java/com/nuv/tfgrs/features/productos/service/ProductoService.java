package com.nuv.tfgrs.features.productos.service;

import com.nuv.tfgrs.features.inventario.dto.request.ActualizarStockRequest;
import com.nuv.tfgrs.features.productos.dto.request.CrearProductoRequest;
import com.nuv.tfgrs.features.productos.dto.request.EditarProductoRequest;
import com.nuv.tfgrs.features.common.dto.response.PagedResponse;
import com.nuv.tfgrs.features.productos.dto.response.ProductoResponse;
import java.util.List;

public interface ProductoService {
    List<ProductoResponse> listarTodos();
    PagedResponse<ProductoResponse> listarPaginado(
            String search,
            Long categoriaId,
            Long providerId,
            String estado,
            Integer limit,
            Integer offset
    );
    List<ProductoResponse> listarBajoStock();
    ProductoResponse buscarPorId(Long id);
    ProductoResponse crear(CrearProductoRequest request);
    ProductoResponse actualizar(Long id, EditarProductoRequest request);
    ProductoResponse actualizarStock(Long id, ActualizarStockRequest request);
    List<String> listarBundlesVinculados(Long id);
    int desvincularDeBundles(Long id);
    void eliminar(Long id);
}
