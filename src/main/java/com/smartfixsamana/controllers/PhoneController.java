package com.smartfixsamana.controllers;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

import com.smartfixsamana.models.entities.Phone;
import com.smartfixsamana.models.services.PhoneService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/phones")
public class PhoneController {

    private final PhoneService phoneService;

    public PhoneController(PhoneService phoneService) {
        this.phoneService = phoneService;
    }
    @GetMapping("/count")
    public long countPhones() {
        return this.phoneService.countAll();
    }

    @GetMapping
    public List<Phone> getPhones() {

        return phoneService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Phone> findPhoneById(@PathVariable Long id) {

        return phoneService.findById(id);

    }

    @PostMapping()
    public ResponseEntity<?> create(@Valid @RequestBody Phone phone, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return validation(bindingResult);
        }
        Phone newPhone = phoneService.create(phone);
        return ResponseEntity.status(HttpStatus.CREATED).body(newPhone);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@Valid @PathVariable Long id, @RequestBody Phone updatePhone,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return validation(bindingResult);
        }
        Phone newPhoneUpdate = phoneService.update(id, updatePhone);
        return ResponseEntity.ok(newPhoneUpdate);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        phoneService.delete(id);
        return ResponseEntity.ok().body("Celular eliminado con exito");
    }

     @GetMapping("/search")
    public ResponseEntity<Page<Phone>> findByKeyword(@RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "4") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Phone> results = phoneService.findByKeyword(keyword, pageable);
        return ResponseEntity.ok(results);
    }

    private ResponseEntity<?> validation(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();

        bindingResult.getFieldErrors().forEach(error -> errors.put(error.getField(), "El campo " + error.getField() + " " + error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

}
