package com.smartfixsamana.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartfixsamana.models.entities.InventoryMovement;
import com.smartfixsamana.models.enums.MovementType;

public interface IInventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findByPartCatalogIdOrderByCreatedAtDesc(Long partCatalogId);

    List<InventoryMovement> findByRepairIdOrderByCreatedAtDesc(Long repairId);

    /**
     * Paginated search with optional filters for partCatalogId, movementType, and date range.
     */
    @Query("SELECT im FROM InventoryMovement im WHERE " +
           "(:partCatalogId IS NULL OR im.partCatalog.id = :partCatalogId) AND " +
           "(:movementType IS NULL OR im.movementType = :movementType) AND " +
           "(:dateFrom IS NULL OR im.createdAt >= :dateFrom) AND " +
           "(:dateTo IS NULL OR im.createdAt <= :dateTo)")
    Page<InventoryMovement> searchMovements(
            @Param("partCatalogId") Long partCatalogId,
            @Param("movementType") MovementType movementType,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);

    /**
     * Find all movements with pagination
     */
    Page<InventoryMovement> findAll(Pageable pageable);
}
