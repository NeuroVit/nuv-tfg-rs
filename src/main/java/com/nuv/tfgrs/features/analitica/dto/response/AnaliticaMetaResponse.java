package com.nuv.tfgrs.features.analitica.dto.response;

import java.time.LocalDateTime;

public record AnaliticaMetaResponse(
        String source,
        boolean seedEnabled,
        LocalDateTime generatedAt,
        int windowDays
) {
}
