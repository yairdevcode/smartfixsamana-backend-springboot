package com.smartfixsamana.controllers;

import com.smartfixsamana.models.dto.SettlementResponse;
import com.smartfixsamana.models.entities.Settlement;
import com.smartfixsamana.models.services.ExternalRepairExcelService;
import com.smartfixsamana.models.services.SettlementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementService settlementService;
    private final ExternalRepairExcelService excelService;

    public SettlementController(SettlementService settlementService,
                                 ExternalRepairExcelService excelService) {
        this.settlementService = settlementService;
        this.excelService = excelService;
    }

    @GetMapping
    public ResponseEntity<List<SettlementResponse>> getAll() {
        List<SettlementResponse> settlements = settlementService.findAll()
                .stream()
                .map(SettlementResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(settlements);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SettlementResponse> getById(@PathVariable Long id) {
        Settlement settlement = settlementService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Liquidación no encontrada"));
        return ResponseEntity.ok(SettlementResponse.fromEntity(settlement));
    }

    @PostMapping
    public ResponseEntity<SettlementResponse> create(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Settlement settlement = settlementService.createSettlement(startDate, endDate);
            return ResponseEntity.status(HttpStatus.CREATED).body(SettlementResponse.fromEntity(settlement));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportSettlement(@PathVariable Long id) {
        try {
            Settlement settlement = settlementService.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Liquidación no encontrada"));

            byte[] excelBytes = excelService.exportToExcel(settlement.getRepairs(), settlement.getStartDate());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment",
                    "liquidacion_" + settlement.getStartDate() + "_" + settlement.getEndDate() + ".xlsx");

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al exportar: " + e.getMessage());
        }
    }
}
