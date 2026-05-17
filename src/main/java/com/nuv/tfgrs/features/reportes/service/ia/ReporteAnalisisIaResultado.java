package com.nuv.tfgrs.features.reportes.service.ia;

import java.time.LocalDateTime;

public record ReporteAnalisisIaResultado(
        String analisis,
        ReporteAnalisisIaEstado estado,
        String modelo,
        LocalDateTime generadoEn,
        String error
) {
}
