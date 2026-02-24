package com.smartfixsamana.controllers;

import com.smartfixsamana.models.dto.ExternalRepairDTO;
import com.smartfixsamana.models.dto.ExternalRepairResponse;
import com.smartfixsamana.models.dto.ImportReconciliationResponse;
import com.smartfixsamana.models.entities.ExternalRepair;
import com.smartfixsamana.models.enums.ExternalRepairStatus;
import com.smartfixsamana.models.services.ExternalRepairExcelService;
import com.smartfixsamana.models.services.ExternalRepairExcelService.ExcelImportRow;
import com.smartfixsamana.models.services.ExternalRepairService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/external-repairs")
public class ExternalRepairController {

    private final ExternalRepairService externalRepairService;
    private final ExternalRepairExcelService excelService;

    public ExternalRepairController(ExternalRepairService externalRepairService,
                                     ExternalRepairExcelService excelService) {
        this.externalRepairService = externalRepairService;
        this.excelService = excelService;
    }

    @GetMapping
    public ResponseEntity<Page<ExternalRepairResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) ExternalRepairStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Page<ExternalRepair> result = externalRepairService.findAllPaginated(
                page, size, sortBy, sortDirection, status, startDate, endDate);

        Page<ExternalRepairResponse> response = result.map(ExternalRepairResponse::fromEntity);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExternalRepairResponse> getById(@PathVariable Long id) {
        ExternalRepair repair = externalRepairService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reparación externa no encontrada"));
        return ResponseEntity.ok(ExternalRepairResponse.fromEntity(repair));
    }

    @PostMapping
    public ResponseEntity<ExternalRepairResponse> create(@Valid @RequestBody ExternalRepairDTO dto, BindingResult result) {
        if (result.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos inválidos");
        }
        ExternalRepair saved = externalRepairService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ExternalRepairResponse.fromEntity(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExternalRepairResponse> update(@PathVariable Long id,
                                                          @Valid @RequestBody ExternalRepairDTO dto,
                                                          BindingResult result) {
        if (result.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos inválidos");
        }
        ExternalRepair updated = externalRepairService.update(id, dto);
        return ResponseEntity.ok(ExternalRepairResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        externalRepairService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reparación externa no encontrada"));
        externalRepairService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) ExternalRepairStatus status) {
        try {
            List<ExternalRepair> repairs;
            if (startDate != null && endDate != null && status != null) {
                repairs = externalRepairService.findByStatusAndDateRange(status, startDate, endDate);
            } else if (startDate != null && endDate != null) {
                repairs = externalRepairService.findByDateRange(startDate, endDate);
            } else {
                repairs = externalRepairService.findByDateRange(
                        startDate != null ? startDate : LocalDate.now().minusMonths(1),
                        endDate != null ? endDate : LocalDate.now());
            }

            byte[] excelBytes = excelService.exportToExcel(repairs);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "reparaciones_externas.xlsx");

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al exportar Excel: " + e.getMessage());
        }
    }

    @PostMapping("/import")
    public ResponseEntity<ImportReconciliationResponse> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<ExcelImportRow> importedRows = excelService.parseImportRows(file.getInputStream());
            ImportReconciliationResponse preview = externalRepairService.previewReconciliation(importedRows, startDate, endDate);
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error al leer archivo Excel: " + e.getMessage());
        }
    }

    @PostMapping("/import/confirm")
    public ResponseEntity<ImportReconciliationResponse> confirmImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<ExcelImportRow> importedRows = excelService.parseImportRows(file.getInputStream());
            ImportReconciliationResponse result = externalRepairService.applyReconciliation(importedRows, startDate, endDate);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error al procesar importación: " + e.getMessage());
        }
    }
}
