package com.nuv.tfgrs.features.analitica.dto.response;

import java.math.BigDecimal;

public record FinanzasResumenResponse(
        Integer days,
        BigDecimal ventas30d,
        BigDecimal compras30d,
        BigDecimal flujoNeto30d,
        BigDecimal cogsReal30d,
        BigDecimal margenReal30d,
        BigDecimal valorStockVenta,
        BigDecimal costoStockReal,
        BigDecimal diferenciaValorVsCosto,
        Long ventasCount30d,
        Long ordenesRecibidasCount30d,
        BigDecimal margenBrutoPct,
        BigDecimal markupPct,
        BigDecimal ticketPromedio,
        BigDecimal inventarioPromedioCosto,
        BigDecimal rotacionInventario,
        BigDecimal diasInventario,
        BigDecimal gmroi,
        BigDecimal ventasPromedioDiarias
) {
}
