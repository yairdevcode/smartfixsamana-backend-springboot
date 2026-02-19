package com.smartfixsamana.models.dto;

public record PartCatalogDTO(
        String name,
        String description,
        Long partTypeId,
        Long phoneId,
        Integer quantity,
        Integer minStock,
        Double purchasePrice,
        Double salePrice
) {
}
