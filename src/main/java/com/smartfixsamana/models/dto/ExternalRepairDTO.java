package com.smartfixsamana.models.dto;

import com.smartfixsamana.models.enums.ExternalRepairStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ExternalRepairDTO(
        @NotBlank String clientName,
        @NotBlank String phoneBrand,
        @NotBlank String solution,
        @NotNull Double repairPrice,
        Double partCost,
        @NotNull ExternalRepairStatus status,
        @NotNull LocalDate date,
        String notes
) {}
