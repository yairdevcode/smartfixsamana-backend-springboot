package com.smartfixsamana.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartfixsamana.models.dto.RepairPartRequest;
import com.smartfixsamana.models.dto.RepairPartResponse;
import com.smartfixsamana.models.entities.RepairPart;
import com.smartfixsamana.models.services.RepairPartService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/repairs")
public class RepairPartController {

    private final RepairPartService repairPartService;

    public RepairPartController(RepairPartService repairPartService) {
        this.repairPartService = repairPartService;
    }

    /**
     * Add a part to a repair.
     * POST /api/repairs/{repairId}/parts
     */
    @PostMapping("/{repairId}/parts")
    public ResponseEntity<?> addPartToRepair(
            @PathVariable Long repairId,
            @Valid @RequestBody RepairPartRequest request,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return validation(bindingResult);
        }
        RepairPart repairPart = repairPartService.addPartToRepair(repairId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(RepairPartResponse.fromEntity(repairPart));
    }

    /**
     * Remove a part from a repair.
     * DELETE /api/repairs/{repairId}/parts/{repairPartId}
     */
    @DeleteMapping("/{repairId}/parts/{repairPartId}")
    public ResponseEntity<Void> removePartFromRepair(
            @PathVariable Long repairId,
            @PathVariable Long repairPartId) {
        repairPartService.removePartFromRepair(repairPartId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all parts for a repair.
     * GET /api/repairs/{repairId}/parts
     */
    @GetMapping("/{repairId}/parts")
    public List<RepairPartResponse> getPartsForRepair(@PathVariable Long repairId) {
        return repairPartService.getPartsByRepair(repairId).stream()
                .map(RepairPartResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private ResponseEntity<?> validation(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        bindingResult.getFieldErrors().forEach(error ->
            errors.put(error.getField(), "El campo " + error.getField() + " " + error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }
}
