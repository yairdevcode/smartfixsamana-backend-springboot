package com.smartfixsamana.controllers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartfixsamana.models.dto.InventoryMovementDTO;
import com.smartfixsamana.models.entities.InventoryMovement;
import com.smartfixsamana.models.enums.MovementType;
import com.smartfixsamana.models.services.InventoryMovementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/inventory-movements")
public class InventoryMovementController {

    private final InventoryMovementService inventoryMovementService;

    public InventoryMovementController(InventoryMovementService inventoryMovementService) {
        this.inventoryMovementService = inventoryMovementService;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody InventoryMovementDTO movementDTO,
                                    BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return validation(bindingResult);
        }
        InventoryMovement newMovement = inventoryMovementService.save(movementDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(newMovement);
    }

    @GetMapping
    public ResponseEntity<Page<InventoryMovement>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) Long partCatalogId,
            @RequestParam(required = false) String movementType,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {

        MovementType type = null;
        if (movementType != null && !movementType.isEmpty()) {
            try {
                type = MovementType.valueOf(movementType.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid movement type, ignore filter
            }
        }

        LocalDateTime from = null;
        LocalDateTime to = null;
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        if (dateFrom != null && !dateFrom.isEmpty()) {
            try {
                // Handle date-only format (YYYY-MM-DD)
                if (dateFrom.length() == 10) {
                    from = LocalDateTime.parse(dateFrom + "T00:00:00", formatter);
                } else {
                    from = LocalDateTime.parse(dateFrom, formatter);
                }
            } catch (Exception e) {
                // Invalid date, ignore filter
            }
        }

        if (dateTo != null && !dateTo.isEmpty()) {
            try {
                // Handle date-only format (YYYY-MM-DD)
                if (dateTo.length() == 10) {
                    to = LocalDateTime.parse(dateTo + "T23:59:59", formatter);
                } else {
                    to = LocalDateTime.parse(dateTo, formatter);
                }
            } catch (Exception e) {
                // Invalid date, ignore filter
            }
        }

        Page<InventoryMovement> result = inventoryMovementService.findAllPaginated(
            page, size, sortBy, sortDirection, partCatalogId, type, from, to
        );
        return ResponseEntity.ok(result);
    }

    /**
     * Get all movements without pagination (legacy endpoint for backwards compatibility)
     */
    @GetMapping("/all")
    public List<InventoryMovement> findAllWithoutPagination() {
        return inventoryMovementService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryMovement> findById(@PathVariable Long id) {
        Optional<InventoryMovement> movement = inventoryMovementService.findById(id);
        return movement.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/part-catalog/{id}")
    public List<InventoryMovement> findByPartCatalogId(@PathVariable Long id) {
        return inventoryMovementService.findByPartCatalogId(id);
    }

    private ResponseEntity<?> validation(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        bindingResult.getFieldErrors().forEach(error ->
            errors.put(error.getField(), "El campo " + error.getField() + " " + error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }
}
