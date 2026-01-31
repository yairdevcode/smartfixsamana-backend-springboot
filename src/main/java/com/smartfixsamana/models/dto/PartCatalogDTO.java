package com.smartfixsamana.models.dto;

import jakarta.validation.constraints.NotBlank;

public record PartCatalogDTO(
        @NotBlank String name,
        String description,
        Long phoneId,
        Integer quantity,
        Integer minStock,
        Double purchasePrice,
        Double salePrice
) {
}
