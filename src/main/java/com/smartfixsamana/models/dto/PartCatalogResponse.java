package com.smartfixsamana.models.dto;

import com.smartfixsamana.models.entities.PartCatalog;

public record PartCatalogResponse(
        Long id,
        String name,
        String description,
        Long phoneId,
        String phoneBrand,
        String phoneModel,
        Integer quantity,
        Integer minStock,
        Double purchasePrice,
        Double salePrice,
        Boolean isLowStock
) {
    public static PartCatalogResponse fromEntity(PartCatalog partCatalog) {
        Long phoneId = null;
        String phoneBrand = null;
        String phoneModel = null;
        if (partCatalog.getPhone() != null) {
            phoneId = partCatalog.getPhone().getId();
            phoneBrand = partCatalog.getPhone().getBrand();
            phoneModel = partCatalog.getPhone().getModel();
        }
        boolean isLowStock = partCatalog.getQuantity() <= partCatalog.getMinStock();
        return new PartCatalogResponse(
                partCatalog.getId(),
                partCatalog.getName(),
                partCatalog.getDescription(),
                phoneId,
                phoneBrand,
                phoneModel,
                partCatalog.getQuantity(),
                partCatalog.getMinStock(),
                partCatalog.getPurchasePrice(),
                partCatalog.getSalePrice(),
                isLowStock
        );
    }
}
