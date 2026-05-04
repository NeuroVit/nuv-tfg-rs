package com.nuv.tfgrs.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VelocidadResumenRowResponse(
        LocalDate movementDate,
        Integer unitsSold,
        BigDecimal revenue
) {
}
