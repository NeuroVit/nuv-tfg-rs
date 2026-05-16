package com.nuv.tfgrs.features.analitica.dto.response;

import java.time.LocalDate;

public record ProductCreationRowResponse(
        LocalDate creationDate,
        Long productsCreated
) {
}
