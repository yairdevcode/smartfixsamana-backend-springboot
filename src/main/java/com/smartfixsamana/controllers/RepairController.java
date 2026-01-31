package com.smartfixsamana.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.smartfixsamana.models.dto.RepairDTO;
import com.smartfixsamana.models.entities.Repair;
import com.smartfixsamana.models.services.RepairService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/repairs")
public class RepairController {

    private final RepairService repairService;

    public RepairController(RepairService repairService) {
        this.repairService = repairService;
    }
    @GetMapping("/count")
    public Long countRepair() {
        return repairService.countAll();
    }

    @GetMapping
    public List<Repair> findAll() {
        return repairService.getAll();
    }

    @GetMapping("/{id}")
    public Optional<Repair> getById(@PathVariable Long id) {
        return repairService.getById(id);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody RepairDTO repairDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return validation(bindingResult);
        }
        Repair newRepair = repairService.save(repairDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(newRepair);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Repair> update(@PathVariable Long id, @RequestBody RepairDTO repairDTO) {
        Repair updateRepair = repairService.update(id, repairDTO);
        return ResponseEntity.ok(updateRepair);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repairService.delete(id);
        return ResponseEntity.ok().body("Eliminado con éxito");
    }

    /**
     * Update only the labor cost for a repair.
     * PATCH /repairs/{id}/labor-cost
     */
    @PatchMapping("/{id}/labor-cost")
    public ResponseEntity<Repair> updateLaborCost(@PathVariable Long id, @RequestBody Map<String, Double> body) {
        Double laborCost = body.get("laborCost");
        Repair updatedRepair = repairService.updateLaborCost(id, laborCost);
        return ResponseEntity.ok(updatedRepair);
    }

    // Búsqueda paginada por keyword (cliente o celular)
    @GetMapping("/search")
    public ResponseEntity<Page<Repair>> findByKeyword(@RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "4") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Repair> results = repairService.findByKeyword(keyword, pageable);
        return ResponseEntity.ok(results);
    }

    private ResponseEntity<?> validation(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        bindingResult.getFieldErrors().forEach(error -> errors.put(error.getField(), "El campo " + error.getField() + " " + error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }
}
