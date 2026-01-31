package com.smartfixsamana.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartfixsamana.models.dto.PartCatalogDTO;
import com.smartfixsamana.models.dto.PartCatalogResponse;
import com.smartfixsamana.models.entities.PartCatalog;
import com.smartfixsamana.models.services.PartCatalogService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/parts-catalog")
public class PartCatalogController {

    private final PartCatalogService partCatalogService;

    public PartCatalogController(PartCatalogService partCatalogService) {
        this.partCatalogService = partCatalogService;
    }

    @GetMapping
    public ResponseEntity<Page<PartCatalogResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long phoneId) {

        Page<PartCatalog> result = partCatalogService.findAllPaginated(
            page, size, sortBy, sortDirection, name, phoneId
        );
        Page<PartCatalogResponse> response = result.map(PartCatalogResponse::fromEntity);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all parts without pagination (legacy endpoint for backwards compatibility)
     */
    @GetMapping("/all")
    public List<PartCatalogResponse> findAllWithoutPagination() {
        return partCatalogService.getAll().stream()
                .map(PartCatalogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartCatalogResponse> findById(@PathVariable Long id) {
        return partCatalogService.findById(id)
                .map(PartCatalogResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PartCatalogResponse>> searchParts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long phoneId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Page<PartCatalog> result = partCatalogService.findAllPaginated(
            page, size, sortBy, sortDirection, name, phoneId
        );
        Page<PartCatalogResponse> response = result.map(PartCatalogResponse::fromEntity);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/available")
    public List<PartCatalogResponse> getAvailableParts() {
        return partCatalogService.getAvailableParts().stream()
                .map(PartCatalogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/low-stock")
    public List<PartCatalogResponse> findLowStock() {
        return partCatalogService.getLowStockParts().stream()
                .map(PartCatalogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/by-phone/{phoneId}")
    public List<PartCatalogResponse> findByPhone(@PathVariable Long phoneId) {
        return partCatalogService.searchParts(null, phoneId).stream()
                .map(PartCatalogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody PartCatalogDTO partCatalogDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return validation(bindingResult);
        }
        PartCatalog saved = partCatalogService.save(partCatalogDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(PartCatalogResponse.fromEntity(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody PartCatalogDTO partCatalogDTO,
                                    BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return validation(bindingResult);
        }
        PartCatalog updated = partCatalogService.update(id, partCatalogDTO);
        return ResponseEntity.ok(PartCatalogResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        partCatalogService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> validation(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        bindingResult.getFieldErrors().forEach(error ->
            errors.put(error.getField(), "El campo " + error.getField() + " " + error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

}
