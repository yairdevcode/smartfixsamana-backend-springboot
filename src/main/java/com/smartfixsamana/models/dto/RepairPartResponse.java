package com.smartfixsamana.models.dto;

import java.time.LocalDateTime;

import com.smartfixsamana.models.entities.RepairPart;

public record RepairPartResponse(
        Long id,
        Long partCatalogId,
        String partCatalogName,
        String phoneBrand,
        String phoneModel,
        Integer quantity,
        Double priceCharged,
        LocalDateTime createdAt
) {
    public static RepairPartResponse fromEntity(RepairPart repairPart) {
        String phoneBrand = null;
        String phoneModel = null;
        if (repairPart.getPartCatalog().getPhone() != null) {
            phoneBrand = repairPart.getPartCatalog().getPhone().getBrand();
            phoneModel = repairPart.getPartCatalog().getPhone().getModel();
        }
        return new RepairPartResponse(
                repairPart.getId(),
                repairPart.getPartCatalog().getId(),
                repairPart.getPartCatalog().getName(),
                phoneBrand,
                phoneModel,
                repairPart.getQuantity(),
                repairPart.getPriceCharged(),
                repairPart.getCreatedAt()
        );
    }
}
