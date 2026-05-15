package com.nuv.tfgrs.features.tareas.service;

import com.nuv.tfgrs.features.tareas.dto.request.ActualizarEstadoTareaRequest;
import com.nuv.tfgrs.features.tareas.dto.request.CrearTareaRequest;
import com.nuv.tfgrs.features.common.dto.response.PagedResponse;
import com.nuv.tfgrs.features.tareas.dto.response.TareaResumenResponse;
import com.nuv.tfgrs.features.tareas.dto.response.TareaResponse;

import java.util.List;

public interface TareaService {
    List<TareaResponse> listar(String estado);
    PagedResponse<TareaResponse> listarPaginado(String estado, Integer limit, Integer offset);
    TareaResumenResponse resumen();
    TareaResponse crear(CrearTareaRequest request, String username);
    TareaResponse actualizarEstado(Long id, ActualizarEstadoTareaRequest request);
    void eliminar(Long id);
}
