package com.smartfixsamana.models.dto;

import com.smartfixsamana.models.entities.Settlement;
import com.smartfixsamana.models.enums.SettlementStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SettlementResponse(
        Long id,
        LocalDate startDate,
        LocalDate endDate,
        Double totalRepairPrice,
        Double totalPartCost,
        Double totalMyShare,
        Double totalStoreShare,
        SettlementStatus status,
        LocalDateTime createdAt,
        int repairCount,
        String warning
) {
    public static SettlementResponse fromEntity(Settlement entity) {
        return fromEntity(entity, null);
    }

    public static SettlementResponse fromEntity(Settlement entity, String warning) {
        return new SettlementResponse(
                entity.getId(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getTotalRepairPrice(),
                entity.getTotalPartCost(),
                entity.getTotalMyShare(),
                entity.getTotalStoreShare(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getRepairs() != null ? entity.getRepairs().size() : 0,
                warning
        );
    }
}
