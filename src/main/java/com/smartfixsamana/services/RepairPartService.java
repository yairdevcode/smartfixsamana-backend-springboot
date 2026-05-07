package com.smartfixsamana.services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.smartfixsamana.models.dto.RepairPartRequest;
import com.smartfixsamana.models.entities.PartCatalog;
import com.smartfixsamana.models.entities.Repair;
import com.smartfixsamana.models.entities.RepairPart;
import com.smartfixsamana.repositories.IRepairPartRepository;
import com.smartfixsamana.repositories.IRepairRepository;

@Service
public class RepairPartService {

    private final IRepairPartRepository repairPartRepository;
    private final IRepairRepository repairRepository;
    private final PartCatalogService partCatalogService;
    private final InventoryMovementService inventoryMovementService;

    public RepairPartService(IRepairPartRepository repairPartRepository,
                            IRepairRepository repairRepository,
                            PartCatalogService partCatalogService,
                            InventoryMovementService inventoryMovementService) {
        this.repairPartRepository = repairPartRepository;
        this.repairRepository = repairRepository;
        this.partCatalogService = partCatalogService;
        this.inventoryMovementService = inventoryMovementService;
    }

    /**
     * Adds a part to a repair.
     * Validates stock availability, creates RepairPart record,
     * decrements PartCatalog.quantity, creates InventoryMovement with type REPAIR_USE,
     * and updates Repair.totalPartsCost.
     *
     * @param repairId The ID of the repair
     * @param request The request containing partCatalogId, quantity, and priceCharged
     * @return The created RepairPart
     */
    @Transactional
    public RepairPart addPartToRepair(Long repairId, RepairPartRequest request) {
        Repair repair = repairRepository.findById(repairId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Reparación no encontrada con ID: " + repairId));

        PartCatalog partCatalog = partCatalogService.findById(request.partCatalogId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Repuesto no encontrado en el catálogo con ID: " + request.partCatalogId()));

        Integer quantity = request.quantity() != null ? request.quantity() : 1;

        // Validate stock is available
        if (partCatalog.getQuantity() < quantity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stock insuficiente. Disponible: " + partCatalog.getQuantity() +
                    ", Solicitado: " + quantity);
        }

        // Create RepairPart record
        RepairPart repairPart = new RepairPart();
        repairPart.setRepair(repair);
        repairPart.setPartCatalog(partCatalog);
        repairPart.setQuantity(quantity);
        repairPart.setPriceCharged(request.priceCharged() != null ?
                request.priceCharged() : partCatalog.getSalePrice());

        RepairPart savedPart = repairPartRepository.save(repairPart);

        // Create InventoryMovement with type REPAIR_USE (decrements stock internally)
        inventoryMovementService.createRepairUseMovement(
                partCatalog,
                repair,
                quantity,
                "Usado en reparación #" + repairId
        );

        // Update Repair.totalPartsCost
        updateRepairTotalPartsCost(repair);

        return savedPart;
    }

    /**
     * Removes a part from a repair.
     * Deletes RepairPart record, increments PartCatalog.quantity (returns to stock),
     * creates InventoryMovement with type REPAIR_RETURN,
     * and updates Repair.totalPartsCost.
     *
     * @param repairPartId The ID of the RepairPart to remove
     */
    @Transactional
    public void removePartFromRepair(Long repairPartId) {
        RepairPart repairPart = repairPartRepository.findById(repairPartId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Repuesto de reparación no encontrado con ID: " + repairPartId));

        Repair repair = repairPart.getRepair();
        PartCatalog partCatalog = repairPart.getPartCatalog();
        Integer quantity = repairPart.getQuantity();

        // Delete RepairPart record
        repairPartRepository.delete(repairPart);

        // Create InventoryMovement with type REPAIR_RETURN (increments stock internally)
        inventoryMovementService.createRepairReturnMovement(
                partCatalog,
                repair,
                quantity,
                "Devuelto de reparación #" + repair.getId()
        );

        // Update Repair.totalPartsCost
        updateRepairTotalPartsCost(repair);
    }

    /**
     * Gets all parts used in a repair.
     *
     * @param repairId The ID of the repair
     * @return List of RepairPart entries for this repair
     */
    public List<RepairPart> getPartsByRepair(Long repairId) {
        // Verify repair exists
        if (!repairRepository.existsById(repairId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Reparación no encontrada con ID: " + repairId);
        }
        return repairPartRepository.findByRepairId(repairId);
    }

    /**
     * Updates the totalPartsCost and totalCost on a Repair entity.
     */
    private void updateRepairTotalPartsCost(Repair repair) {
        List<RepairPart> parts = repairPartRepository.findByRepairId(repair.getId());
        double totalPartsCost = parts.stream()
                .mapToDouble(p -> (p.getPriceCharged() != null ? p.getPriceCharged() : 0.0) * p.getQuantity())
                .sum();
        repair.setTotalPartsCost(totalPartsCost);
        repair.recalculateTotalCost();
        repairRepository.save(repair);
    }

    /**
     * Finds a RepairPart by ID.
     */
    public RepairPart findById(Long id) {
        return repairPartRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Repuesto de reparación no encontrado con ID: " + id));
    }
}
