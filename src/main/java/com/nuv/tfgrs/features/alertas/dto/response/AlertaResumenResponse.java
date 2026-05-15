package com.nuv.tfgrs.features.alertas.dto.response;

public record AlertaResumenResponse(
        long abiertas,
        long stockBajo,
        long anomaliasPrecio,
        long resueltasUltimos7Dias,
        long criticas,
        long anomalias
) {
}
