package com.smartfixsamana.models.dto;

import java.time.LocalDate;

public record RangeEarningsResponse(
        LocalDate startDate,
        LocalDate endDate,
        Double ownRepairsTotal,
        Double externalRepairsMyShare,
        Double total,
        int ownRepairsCount,
        int externalRepairsCount
) {
}
