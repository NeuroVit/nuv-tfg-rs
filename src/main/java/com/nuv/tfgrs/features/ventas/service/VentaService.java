package com.nuv.tfgrs.features.ventas.service;

import com.nuv.tfgrs.features.ventas.dto.request.CrearVentaRequest;
import com.nuv.tfgrs.features.common.dto.response.PagedResponse;
import com.nuv.tfgrs.features.ventas.dto.response.VentaResponse;

import java.time.LocalDate;
import java.util.List;

public interface VentaService {
    List<VentaResponse> listar(Long clientId, int limit);
    PagedResponse<VentaResponse> listarPaginado(
            Long clientId,
            String search,
            LocalDate fromDate,
            LocalDate toDate,
            Integer limit,
            Integer offset
    );
    VentaResponse crear(CrearVentaRequest request, String actorUsername);
}
