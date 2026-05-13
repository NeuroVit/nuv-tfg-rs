package com.nuv.tfgrs.dto.response;

import java.time.LocalDate;

public record ProductCreationRowResponse(
        LocalDate creationDate,
        Long productsCreated
) {
}
