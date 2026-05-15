package com.nuv.tfgrs.features.alertas.service;

import com.nuv.tfgrs.features.alertas.dto.response.AlertaResumenResponse;
import com.nuv.tfgrs.features.alertas.dto.response.AlertaStockResponse;
import com.nuv.tfgrs.features.common.dto.response.PagedResponse;

import java.util.List;

public interface AlertaStockService {
    List<AlertaStockResponse> listar(Boolean resueltas);
    PagedResponse<AlertaStockResponse> listarPaginado(Boolean resueltas, String search, Integer limit, Integer offset);
    AlertaResumenResponse resumen();
    AlertaStockResponse resolver(Long alertId);
}
