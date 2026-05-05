package com.nuv.tfgrs.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CrearVentaRequest(
        @NotNull Long clientId,
        @Size(max = 500) String notas,
        @Valid List<Item> items,
        @Valid List<BundleItem> bundles,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal descuentoPctGlobal
) {
    public record Item(
            @NotNull Long productId,
            @Positive Integer quantity,
            @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal descuentoPct
    ) {
    }

    public record BundleItem(
            @NotNull Long bundleId,
            @Positive Integer quantity,
            @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal descuentoPctOverride
    ) {
    }
}
