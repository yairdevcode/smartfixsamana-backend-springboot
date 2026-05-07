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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartfixsamana.models.dto.PartTypeDTO;
import com.smartfixsamana.models.dto.PartTypeResponse;
import com.smartfixsamana.models.entities.PartType;
import com.smartfixsamana.services.PartTypeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/part-types")
public class PartTypeController {

    private final PartTypeService partTypeService;

    public PartTypeController(PartTypeService partTypeService) {
        this.partTypeService = partTypeService;
    }

    /**
     * Get all part types ordered by name.
     */
    @GetMapping
    public List<PartTypeResponse> findAll() {
        return partTypeService.getAll().stream()
                .map(PartTypeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get a part type by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PartTypeResponse> findById(@PathVariable Long id) {
        return partTypeService.findById(id)
                .map(PartTypeResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Search part types by name.
     */
    @GetMapping("/search")
    public List<PartTypeResponse> search(@RequestParam String name) {
        return partTypeService.searchByName(name).stream()
                .map(PartTypeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Create a new part type.
     */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody PartTypeDTO partTypeDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return validation(bindingResult);
        }
        PartType saved = partTypeService.save(partTypeDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(PartTypeResponse.fromEntity(saved));
    }

    /**
     * Update an existing part type.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody PartTypeDTO partTypeDTO,
                                    BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return validation(bindingResult);
        }
        PartType updated = partTypeService.update(id, partTypeDTO);
        return ResponseEntity.ok(PartTypeResponse.fromEntity(updated));
    }

    /**
     * Delete a part type by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        partTypeService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> validation(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        bindingResult.getFieldErrors().forEach(error ->
            errors.put(error.getField(), "El campo " + error.getField() + " " + error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

}
