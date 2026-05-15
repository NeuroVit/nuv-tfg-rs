package com.nuv.tfgrs.features.tareas.service;

import com.nuv.tfgrs.features.tareas.dto.request.ActualizarEstadoTareaRequest;
import com.nuv.tfgrs.features.tareas.dto.request.ActualizarAsignadoTareaRequest;
import com.nuv.tfgrs.features.tareas.dto.request.CrearTareaRequest;
import com.nuv.tfgrs.features.common.dto.response.PagedResponse;
import com.nuv.tfgrs.features.tareas.dto.response.TareaAsignableUsuarioResponse;
import com.nuv.tfgrs.features.tareas.dto.response.TareaResumenResponse;
import com.nuv.tfgrs.features.tareas.dto.response.TareaResponse;

import java.util.List;

public interface TareaService {
    List<TareaResponse> listar(String estado);
    PagedResponse<TareaResponse> listarPaginado(String estado, Integer limit, Integer offset);
    TareaResumenResponse resumen();
    List<TareaAsignableUsuarioResponse> listarAsignables();
    TareaResponse crear(CrearTareaRequest request);
    TareaResponse actualizarEstado(Long id, ActualizarEstadoTareaRequest request);
    TareaResponse actualizarAsignado(Long id, ActualizarAsignadoTareaRequest request);
    void eliminar(Long id);
}
