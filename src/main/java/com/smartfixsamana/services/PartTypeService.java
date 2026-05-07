package com.smartfixsamana.services;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.smartfixsamana.models.dto.PartTypeDTO;
import com.smartfixsamana.models.entities.PartType;
import com.smartfixsamana.repositories.IPartTypeRepository;

@Service
public class PartTypeService {

    private final IPartTypeRepository partTypeRepository;

    public PartTypeService(IPartTypeRepository partTypeRepository) {
        this.partTypeRepository = partTypeRepository;
    }

    /**
     * Retrieves all part types ordered by name.
     */
    public List<PartType> getAll() {
        return partTypeRepository.findAllByOrderByNameAsc();
    }

    /**
     * Finds a part type by ID.
     */
    public Optional<PartType> findById(Long id) {
        return partTypeRepository.findById(id);
    }

    /**
     * Finds a part type by name.
     */
    public Optional<PartType> findByName(String name) {
        return partTypeRepository.findByName(name);
    }

    /**
     * Creates a new part type from DTO.
     */
    public PartType save(PartTypeDTO dto) {
        // Check if name already exists
        if (partTypeRepository.existsByName(dto.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un tipo de repuesto con el nombre: " + dto.name());
        }

        PartType partType = new PartType();
        partType.setName(dto.name());
        partType.setDescription(dto.description());

        return partTypeRepository.save(partType);
    }

    /**
     * Updates an existing part type from DTO.
     */
    public PartType update(Long id, PartTypeDTO dto) {
        PartType partType = partTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tipo de repuesto no encontrado con ID: " + id));

        // Check if new name conflicts with another existing part type
        Optional<PartType> existingByName = partTypeRepository.findByName(dto.name());
        if (existingByName.isPresent() && !existingByName.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un tipo de repuesto con el nombre: " + dto.name());
        }

        partType.setName(dto.name());
        partType.setDescription(dto.description());

        return partTypeRepository.save(partType);
    }

    /**
     * Deletes a part type by ID.
     */
    public void deleteById(Long id) {
        if (!partTypeRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Tipo de repuesto no encontrado con ID: " + id);
        }
        partTypeRepository.deleteById(id);
    }

    /**
     * Searches part types by name (case-insensitive partial match).
     */
    public List<PartType> searchByName(String name) {
        return partTypeRepository.findByNameContainingIgnoreCase(name);
    }

}
