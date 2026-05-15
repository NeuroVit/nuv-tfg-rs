package com.nuv.tfgrs.service;

import com.nuv.tfgrs.dto.request.ActualizarEstadoTareaRequest;
import com.nuv.tfgrs.dto.request.CrearTareaRequest;
import com.nuv.tfgrs.dto.response.TareaResponse;

import java.util.List;

public interface TareaService {
    List<TareaResponse> listar(String estado);
    TareaResponse crear(CrearTareaRequest request, String username);
    TareaResponse actualizarEstado(Long id, ActualizarEstadoTareaRequest request);
    void eliminar(Long id);
}
