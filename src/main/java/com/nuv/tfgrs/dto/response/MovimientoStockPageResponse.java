package com.nuv.tfgrs.dto.response;

import java.util.List;

public record MovimientoStockPageResponse(
        List<MovimientoStockResponse> content,
        long total,
        int limit,
        int offset
) {
}
