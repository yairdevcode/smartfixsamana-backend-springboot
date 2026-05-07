package com.smartfixsamana.controllers;

import com.smartfixsamana.models.dto.ImportReconciliationResponse;
import com.smartfixsamana.models.dto.SettlementResponse;
import com.smartfixsamana.models.entities.Settlement;
import com.smartfixsamana.services.ExternalRepairExcelService;
import com.smartfixsamana.services.ExternalRepairExcelService.ExcelImportRow;
import com.smartfixsamana.services.ExternalRepairService;
import com.smartfixsamana.services.SettlementService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementService settlementService;
    private final ExternalRepairExcelService excelService;
    private final ExternalRepairService externalRepairService;

    public SettlementController(SettlementService settlementService,
                                 ExternalRepairExcelService excelService,
                                 ExternalRepairService externalRepairService) {
        this.settlementService = settlementService;
        this.excelService = excelService;
        this.externalRepairService = externalRepairService;
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
            Map<String, Object> result = settlementService.createSettlement(startDate, endDate);
            Settlement settlement = (Settlement) result.get("settlement");
            String warning = (String) result.get("warning");
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(SettlementResponse.fromEntity(settlement, warning));
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

    @PostMapping("/{id}/import")
    public ResponseEntity<ImportReconciliationResponse> importExcel(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            Settlement settlement = settlementService.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Liquidación no encontrada"));
            List<ExcelImportRow> importedRows = excelService.parseImportRows(file.getInputStream());
            ImportReconciliationResponse preview = externalRepairService
                    .previewReconciliationBySettlement(importedRows, id, settlement.getStartDate());
            return ResponseEntity.ok(preview);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error al leer archivo Excel: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/import/confirm")
    public ResponseEntity<ImportReconciliationResponse> confirmImport(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            Settlement settlement = settlementService.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Liquidación no encontrada"));
            List<ExcelImportRow> importedRows = excelService.parseImportRows(file.getInputStream());
            ImportReconciliationResponse result = externalRepairService
                    .applyReconciliationBySettlement(importedRows, id, settlement.getStartDate());
            return ResponseEntity.ok(result);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error al procesar importación: " + e.getMessage());
        }
    }
}
