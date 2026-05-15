package com.nuv.tfgrs.service;

import com.nuv.tfgrs.dto.request.CrearReporteProgramadoRequest;
import com.nuv.tfgrs.dto.response.ReporteProgramadoResponse;

import java.util.List;

public interface ReporteProgramadoService {
    List<ReporteProgramadoResponse> listar();
    ReporteProgramadoResponse crear(CrearReporteProgramadoRequest request, String username);
    ReporteProgramadoResponse toggleActivo(Long id);
    void eliminar(Long id);
}
