package com.smartfixsamana.models.dto;

public record EarningsSummaryResponse(
        DailyEarningsResponse today,
        RangeEarningsResponse last30Days,
        RangeEarningsResponse currentMonth
) {
}
