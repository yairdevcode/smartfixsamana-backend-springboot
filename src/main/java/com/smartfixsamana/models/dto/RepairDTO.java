package com.smartfixsamana.models.dto;

import java.time.LocalDate;

public record RepairDTO(
        Long customerId,
        Long phoneId,
        String fault,
        String state,
        LocalDate date,
        Double laborCost
) {
}