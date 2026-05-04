package com.nuv.tfgrs.service;

import com.nuv.tfgrs.dto.request.CrearClienteRequest;
import com.nuv.tfgrs.dto.request.EditarClienteRequest;
import com.nuv.tfgrs.dto.response.ClienteResponse;

import java.util.List;

public interface ClienteService {
    List<ClienteResponse> listar(boolean includeArchived);
    ClienteResponse crear(CrearClienteRequest request);
    ClienteResponse actualizar(Long id, EditarClienteRequest request);
    ClienteResponse archivar(Long id);
    ClienteResponse restaurar(Long id);
}
