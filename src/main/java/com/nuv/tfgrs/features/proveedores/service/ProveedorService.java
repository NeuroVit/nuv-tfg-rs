package com.nuv.tfgrs.features.proveedores.service;

import com.nuv.tfgrs.features.proveedores.dto.request.CrearProveedorRequest;
import com.nuv.tfgrs.features.proveedores.dto.request.EditarProveedorRequest;
import com.nuv.tfgrs.features.common.dto.response.PagedResponse;
import com.nuv.tfgrs.features.proveedores.dto.response.ProveedorDetalleResponse;
import com.nuv.tfgrs.features.proveedores.dto.response.ProveedorResponse;

import java.util.List;

public interface ProveedorService {
    List<ProveedorResponse> listar(boolean includeArchived);
    PagedResponse<ProveedorResponse> listarPaginado(String status, String search, Integer limit, Integer offset);
    ProveedorResponse crear(CrearProveedorRequest request);
    ProveedorResponse actualizar(Long id, EditarProveedorRequest request);
    ProveedorResponse archivar(Long id);
    ProveedorResponse restaurar(Long id);
    ProveedorDetalleResponse detalle(Long id);
}
