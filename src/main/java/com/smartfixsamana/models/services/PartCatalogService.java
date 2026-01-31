package com.smartfixsamana.models.services;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.smartfixsamana.models.dto.PartCatalogDTO;
import com.smartfixsamana.models.entities.PartCatalog;
import com.smartfixsamana.models.entities.Phone;
import com.smartfixsamana.models.repositories.IPartCatalogRepository;

@Service
public class PartCatalogService {

    private final IPartCatalogRepository partCatalogRepository;
    private final PhoneService phoneService;

    public PartCatalogService(IPartCatalogRepository partCatalogRepository, PhoneService phoneService) {
        this.partCatalogRepository = partCatalogRepository;
        this.phoneService = phoneService;
    }

    /**
     * Retrieves all parts from the catalog.
     */
    public List<PartCatalog> getAll() {
        return (List<PartCatalog>) partCatalogRepository.findAll();
    }

    /**
     * Finds a part catalog entry by ID.
     */
    public Optional<PartCatalog> findById(Long id) {
        return partCatalogRepository.findById(id);
    }

    /**
     * Saves a part catalog entity directly.
     */
    public PartCatalog save(PartCatalog partCatalog) {
        return partCatalogRepository.save(partCatalog);
    }

    /**
     * Creates or updates a part catalog entry from DTO.
     */
    public PartCatalog save(PartCatalogDTO dto) {
        PartCatalog partCatalog = new PartCatalog();
        return updateFromDTO(partCatalog, dto);
    }

    /**
     * Updates an existing part catalog entry from DTO.
     */
    public PartCatalog update(Long id, PartCatalogDTO dto) {
        PartCatalog partCatalog = partCatalogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Repuesto no encontrado con ID: " + id));
        return updateFromDTO(partCatalog, dto);
    }

    private PartCatalog updateFromDTO(PartCatalog partCatalog, PartCatalogDTO dto) {
        partCatalog.setName(dto.name());
        partCatalog.setDescription(dto.description());
        partCatalog.setQuantity(dto.quantity() != null ? dto.quantity() : 0);
        partCatalog.setMinStock(dto.minStock() != null ? dto.minStock() : 5);
        partCatalog.setPurchasePrice(dto.purchasePrice());
        partCatalog.setSalePrice(dto.salePrice());

        if (dto.phoneId() != null) {
            Phone phone = phoneService.findById(dto.phoneId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Celular no encontrado con ID: " + dto.phoneId()));
            partCatalog.setPhone(phone);
        } else {
            partCatalog.setPhone(null);
        }

        return partCatalogRepository.save(partCatalog);
    }

    /**
     * Finds parts with stock at or below minimum stock level.
     */
    public List<PartCatalog> findLowStock() {
        return partCatalogRepository.findLowStock();
    }

    /**
     * Deletes a part catalog entry by ID.
     */
    public void deleteById(Long id) {
        partCatalogRepository.deleteById(id);
    }

    /**
     * Searches parts by name and/or phone ID.
     * @param name Optional part name filter (case-insensitive partial match)
     * @param phoneId Optional phone ID filter
     */
    public List<PartCatalog> searchParts(String name, Long phoneId) {
        if (name != null && phoneId != null) {
            return partCatalogRepository.findByNameContainingIgnoreCaseAndPhoneId(name, phoneId);
        } else if (name != null) {
            return partCatalogRepository.findByNameContainingIgnoreCase(name);
        } else if (phoneId != null) {
            return partCatalogRepository.findByPhoneId(phoneId);
        } else {
            return getAll();
        }
    }

    /**
     * Finds all parts with quantity > 0 (available for use).
     */
    public List<PartCatalog> getAvailableParts() {
        return partCatalogRepository.findByQuantityGreaterThan(0);
    }

    /**
     * Finds parts where quantity <= minStock (low stock alert).
     */
    public List<PartCatalog> getLowStockParts() {
        return partCatalogRepository.findLowStock();
    }

    /**
     * Paginated search for parts catalog with optional filters.
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortBy Field to sort by
     * @param sortDirection Sort direction (asc/desc)
     * @param name Optional filter by part name (case-insensitive partial match)
     * @param phoneId Optional filter by phone ID
     */
    public Page<PartCatalog> findAllPaginated(
            int page,
            int size,
            String sortBy,
            String sortDirection,
            String name,
            Long phoneId) {

        Sort sort = Sort.by(
            sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
            sortBy
        );
        Pageable pageable = PageRequest.of(page, size, sort);

        return partCatalogRepository.searchPartsPaginated(name, phoneId, pageable);
    }

    /**
     * Get all parts paginated without filters.
     */
    public Page<PartCatalog> findAllPaginated(int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(
            sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
            sortBy
        );
        Pageable pageable = PageRequest.of(page, size, sort);
        return partCatalogRepository.findAll(pageable);
    }

}
