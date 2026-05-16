package com.nuv.tfgrs.features.clientes.service;

import com.nuv.tfgrs.features.clientes.dto.request.CrearClienteRequest;
import com.nuv.tfgrs.features.clientes.dto.request.EditarClienteRequest;
import com.nuv.tfgrs.features.clientes.dto.response.ClienteResponse;
import com.nuv.tfgrs.features.common.dto.response.PagedResponse;

import java.util.List;

public interface ClienteService {
    List<ClienteResponse> listar(boolean includeArchived);
    PagedResponse<ClienteResponse> listarPaginado(String status, String search, Integer limit, Integer offset);
    ClienteResponse crear(CrearClienteRequest request);
    ClienteResponse actualizar(Long id, EditarClienteRequest request);
    ClienteResponse archivar(Long id);
    ClienteResponse restaurar(Long id);
}
