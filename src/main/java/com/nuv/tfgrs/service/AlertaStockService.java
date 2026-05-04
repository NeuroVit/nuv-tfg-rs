package com.nuv.tfgrs.service;

import com.nuv.tfgrs.dto.response.AlertaStockResponse;

import java.util.List;

public interface AlertaStockService {
    List<AlertaStockResponse> listar(Boolean resueltas);
    AlertaStockResponse resolver(Long alertId);
}
