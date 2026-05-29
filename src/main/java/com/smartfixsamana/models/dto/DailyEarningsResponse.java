package com.smartfixsamana.models.dto;

import java.time.LocalDate;

public record DailyEarningsResponse(
        LocalDate date,
        Double ownRepairsTotal,
        Double externalRepairsMyShare,
        Double total,
        int ownRepairsCount,
        int externalRepairsCount
) {
}
