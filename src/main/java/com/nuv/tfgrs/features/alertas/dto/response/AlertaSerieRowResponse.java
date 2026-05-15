package com.nuv.tfgrs.features.alertas.dto.response;

import java.time.LocalDate;

public record AlertaSerieRowResponse(
        LocalDate bucketDate,
        String alertType,
        Long createdCount,
        Long resolvedCount
) {
}
