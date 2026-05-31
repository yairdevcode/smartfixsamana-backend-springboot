package com.smartfixsamana.controllers;

import com.smartfixsamana.models.dto.DailyEarningsResponse;
import com.smartfixsamana.models.dto.EarningsSummaryResponse;
import com.smartfixsamana.models.dto.RangeEarningsResponse;
import com.smartfixsamana.models.services.EarningsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/earnings")
public class EarningsController {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");

    private final EarningsService earningsService;

    public EarningsController(EarningsService earningsService) {
        this.earningsService = earningsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<EarningsSummaryResponse> getSummary() {
        return ResponseEntity.ok(earningsService.getSummary());
    }

    @GetMapping("/daily")
    public ResponseEntity<DailyEarningsResponse> getDaily(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now(BUSINESS_ZONE);
        }
        return ResponseEntity.ok(earningsService.getDailyEarnings(date));
    }

    @GetMapping("/range")
    public ResponseEntity<RangeEarningsResponse> getRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(earningsService.getRangeEarnings(startDate, endDate));
    }
}
