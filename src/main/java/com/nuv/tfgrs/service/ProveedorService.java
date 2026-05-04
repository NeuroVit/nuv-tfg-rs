package com.nuv.tfgrs.service;

import com.nuv.tfgrs.dto.request.CrearProveedorRequest;
import com.nuv.tfgrs.dto.request.EditarProveedorRequest;
import com.nuv.tfgrs.dto.response.ProveedorDetalleResponse;
import com.nuv.tfgrs.dto.response.ProveedorResponse;

import java.util.List;

public interface ProveedorService {
    List<ProveedorResponse> listar(boolean includeArchived);
    ProveedorResponse crear(CrearProveedorRequest request);
    ProveedorResponse actualizar(Long id, EditarProveedorRequest request);
    ProveedorResponse archivar(Long id);
    ProveedorResponse restaurar(Long id);
    ProveedorDetalleResponse detalle(Long id);
}
