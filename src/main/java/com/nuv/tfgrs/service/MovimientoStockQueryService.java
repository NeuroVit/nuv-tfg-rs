package com.nuv.tfgrs.service;

import com.nuv.tfgrs.dto.response.MovimientoStockPageResponse;

import java.time.LocalDate;

public interface MovimientoStockQueryService {
    MovimientoStockPageResponse listar(
            Long productId,
            String tipo,
            String razon,
            LocalDate desde,
            LocalDate hasta,
            Integer limit,
            Integer offset
    );
}
