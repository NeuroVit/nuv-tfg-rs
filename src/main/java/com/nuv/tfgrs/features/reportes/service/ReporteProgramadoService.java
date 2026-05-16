package com.nuv.tfgrs.features.reportes.service;

import com.nuv.tfgrs.features.reportes.dto.request.CrearReporteProgramadoRequest;
import com.nuv.tfgrs.features.reportes.dto.response.ReporteProgramadoResponse;

import java.util.List;

public interface ReporteProgramadoService {
    List<ReporteProgramadoResponse> listar();
    ReporteProgramadoResponse crear(CrearReporteProgramadoRequest request, String username);
    ReporteProgramadoResponse toggleActivo(Long id);
    void eliminar(Long id);
}
