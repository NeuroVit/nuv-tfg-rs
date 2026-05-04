package com.nuv.tfgrs.service;

import com.nuv.tfgrs.dto.request.CrearVentaRequest;
import com.nuv.tfgrs.dto.response.VentaResponse;

import java.util.List;

public interface VentaService {
    List<VentaResponse> listar(Long clientId, int limit);
    VentaResponse crear(CrearVentaRequest request, String actorUsername);
}
