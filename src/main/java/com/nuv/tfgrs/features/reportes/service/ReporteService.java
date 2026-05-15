package com.nuv.tfgrs.features.reportes.service;

import com.nuv.tfgrs.features.reportes.dto.request.CrearReporteRequest;
import com.nuv.tfgrs.features.reportes.dto.response.ReporteResponse;

import java.util.List;

public interface ReporteService {
    List<ReporteResponse> listar();
    List<ReporteResponse> listarPorTipo(String tipo);
    ReporteResponse generar(CrearReporteRequest request, String username);
    void eliminar(Long id);
}
