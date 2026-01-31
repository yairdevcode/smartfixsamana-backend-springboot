package com.smartfixsamana.models.dto;

import com.smartfixsamana.models.enums.MovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InventoryMovementDTO(
    @NotNull Long partCatalogId,
    @NotNull MovementType movementType,
    @NotNull @Positive Integer quantity,
    String reason,
    String notes
) {
}
