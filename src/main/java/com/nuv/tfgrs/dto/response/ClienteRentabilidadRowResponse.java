package com.nuv.tfgrs.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClienteRentabilidadRowResponse(
        Long clientId,
        String clientName,
        Integer salesCount,
        Integer unitsSold,
        BigDecimal revenue,
        BigDecimal cost,
        BigDecimal profit,
        BigDecimal margin,
        LocalDateTime lastSaleAt,
        BigDecimal avgTicket
) {
}
