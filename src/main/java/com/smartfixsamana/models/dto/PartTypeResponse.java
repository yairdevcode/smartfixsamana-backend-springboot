package com.smartfixsamana.models.dto;

import java.time.LocalDateTime;

import com.smartfixsamana.models.entities.PartType;

public record PartTypeResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PartTypeResponse fromEntity(PartType partType) {
        return new PartTypeResponse(
                partType.getId(),
                partType.getName(),
                partType.getDescription(),
                partType.getCreatedAt(),
                partType.getUpdatedAt()
        );
    }
}
