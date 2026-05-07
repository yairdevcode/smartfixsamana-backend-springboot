package com.smartfixsamana.models.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.smartfixsamana.models.dto.InventoryMovementDTO;
import com.smartfixsamana.models.entities.InventoryMovement;
import com.smartfixsamana.models.entities.PartCatalog;
import com.smartfixsamana.models.entities.Repair;
import com.smartfixsamana.models.enums.MovementType;
import com.smartfixsamana.models.repositories.IInventoryMovementRepository;

@Service
public class InventoryMovementService {

    private final IInventoryMovementRepository inventoryMovementRepository;
    private final PartCatalogService partCatalogService;

    public InventoryMovementService(IInventoryMovementRepository inventoryMovementRepository,
                                   PartCatalogService partCatalogService) {
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.partCatalogService = partCatalogService;
    }

    /**
     * Determines if a movement type increases stock.
     */
    private boolean isStockIncrease(MovementType type) {
        return type == MovementType.PURCHASE ||
               type == MovementType.REPAIR_RETURN ||
               type == MovementType.ADJUSTMENT;
    }

    /**
     * Determines if a movement type decreases stock.
     */
    private boolean isStockDecrease(MovementType type) {
        return type == MovementType.SALE ||
               type == MovementType.REPAIR_USE ||
               type == MovementType.DAMAGE;
    }

    @Transactional
    public InventoryMovement save(InventoryMovementDTO movementDTO) {
        PartCatalog partCatalog = partCatalogService.findById(movementDTO.partCatalogId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Repuesto no encontrado en el catálogo"));

        // Validate sufficient stock for stock-decreasing movements
        if (isStockDecrease(movementDTO.movementType())) {
            if (partCatalog.getQuantity() < movementDTO.quantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stock insuficiente. Disponible: " + partCatalog.getQuantity() +
                    ", Solicitado: " + movementDTO.quantity());
            }
        }

        // Create movement
        InventoryMovement movement = new InventoryMovement();
        movement.setPartCatalog(partCatalog);
        movement.setMovementType(movementDTO.movementType());
        movement.setQuantity(movementDTO.quantity());
        movement.setReason(movementDTO.reason());
        movement.setNotes(movementDTO.notes());

        // Update stock based on movement type
        if (isStockIncrease(movementDTO.movementType())) {
            partCatalog.setQuantity(partCatalog.getQuantity() + movementDTO.quantity());
        } else if (isStockDecrease(movementDTO.movementType())) {
            partCatalog.setQuantity(partCatalog.getQuantity() - movementDTO.quantity());
        }
        // ADJUSTMENT can be + or - depending on context, handled by caller

        partCatalogService.save(partCatalog);
        return inventoryMovementRepository.save(movement);
    }

    /**
     * Creates a REPAIR_USE movement when a part is used in a repair.
     */
    @Transactional
    public InventoryMovement createRepairUseMovement(PartCatalog partCatalog, Repair repair,
                                                      Integer quantity, String reason) {
        // Validate sufficient stock
        if (partCatalog.getQuantity() < quantity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Stock insuficiente. Disponible: " + partCatalog.getQuantity() +
                ", Solicitado: " + quantity);
        }

        // Create movement
        InventoryMovement movement = new InventoryMovement();
        movement.setPartCatalog(partCatalog);
        movement.setRepair(repair);
        movement.setMovementType(MovementType.REPAIR_USE);
        movement.setQuantity(quantity);
        movement.setReason(reason);

        // Update stock
        partCatalog.setQuantity(partCatalog.getQuantity() - quantity);
        partCatalogService.save(partCatalog);

        return inventoryMovementRepository.save(movement);
    }

    /**
     * Creates a REPAIR_RETURN movement when a part is removed from a repair.
     */
    @Transactional
    public InventoryMovement createRepairReturnMovement(PartCatalog partCatalog, Repair repair,
                                                         Integer quantity, String reason) {
        // Create movement
        InventoryMovement movement = new InventoryMovement();
        movement.setPartCatalog(partCatalog);
        movement.setRepair(repair);
        movement.setMovementType(MovementType.REPAIR_RETURN);
        movement.setQuantity(quantity);
        movement.setReason(reason);

        // Return stock
        partCatalog.setQuantity(partCatalog.getQuantity() + quantity);
        partCatalogService.save(partCatalog);

        return inventoryMovementRepository.save(movement);
    }

    public List<InventoryMovement> findAll() {
        return (List<InventoryMovement>) inventoryMovementRepository.findAll();
    }

    public Optional<InventoryMovement> findById(Long id) {
        return inventoryMovementRepository.findById(id);
    }

    public List<InventoryMovement> findByPartCatalogId(Long partCatalogId) {
        return inventoryMovementRepository.findByPartCatalogIdOrderByCreatedAtDesc(partCatalogId);
    }

    public List<InventoryMovement> findByRepairId(Long repairId) {
        return inventoryMovementRepository.findByRepairIdOrderByCreatedAtDesc(repairId);
    }

    /**
     * Paginated search for inventory movements with optional filters.
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortBy Field to sort by
     * @param sortDirection Sort direction (asc/desc)
     * @param partCatalogId Optional filter by part catalog ID
     * @param movementType Optional filter by movement type
     * @param dateFrom Optional filter for movements after this date
     * @param dateTo Optional filter for movements before this date
     */
    public Page<InventoryMovement> findAllPaginated(
            int page,
            int size,
            String sortBy,
            String sortDirection,
            Long partCatalogId,
            MovementType movementType,
            LocalDateTime dateFrom,
            LocalDateTime dateTo) {

        Sort sort = Sort.by(
            sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
            sortBy
        );
        Pageable pageable = PageRequest.of(page, size, sort);

        return inventoryMovementRepository.searchMovements(
            partCatalogId,
            movementType,
            dateFrom,
            dateTo,
            pageable
        );
    }

    /**
     * Get all movements paginated without filters.
     */
    public Page<InventoryMovement> findAllPaginated(int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(
            sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
            sortBy
        );
        Pageable pageable = PageRequest.of(page, size, sort);
        return inventoryMovementRepository.findAll(pageable);
    }
}
