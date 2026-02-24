package com.smartfixsamana.models.dto;

import com.smartfixsamana.models.entities.ExternalRepair;
import com.smartfixsamana.models.enums.ExternalRepairStatus;

import java.time.LocalDate;

public record ExternalRepairResponse(
        Long id,
        String clientName,
        String phoneBrand,
        String solution,
        Double repairPrice,
        Double partCost,
        ExternalRepairStatus status,
        LocalDate date,
        String notes,
        Long settlementId,
        Double netProfit,
        Double myShare,
        Double storeShare
) {
    public static ExternalRepairResponse fromEntity(ExternalRepair entity) {
        return new ExternalRepairResponse(
                entity.getId(),
                entity.getClientName(),
                entity.getPhoneBrand(),
                entity.getSolution(),
                entity.getRepairPrice(),
                entity.getPartCost(),
                entity.getStatus(),
                entity.getDate(),
                entity.getNotes(),
                entity.getSettlement() != null ? entity.getSettlement().getId() : null,
                entity.getNetProfit(),
                entity.getMyShare(),
                entity.getStoreShare()
        );
    }
}
