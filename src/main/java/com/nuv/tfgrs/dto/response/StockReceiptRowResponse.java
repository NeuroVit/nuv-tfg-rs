package com.nuv.tfgrs.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockReceiptRowResponse(
        LocalDate receiptDate,
        Long unitsReceived,
        BigDecimal valueReceived
) {
}
