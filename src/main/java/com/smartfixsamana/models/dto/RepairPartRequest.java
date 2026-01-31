package com.smartfixsamana.models.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RepairPartRequest(
        @NotNull Long partCatalogId,
        @Positive Integer quantity,
        Double priceCharged
) {
}
