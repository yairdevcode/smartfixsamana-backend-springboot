package com.smartfixsamana.models.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.smartfixsamana.models.entities.RepairPart;

public interface IRepairPartRepository extends CrudRepository<RepairPart, Long> {

    List<RepairPart> findByRepairId(Long repairId);

    List<RepairPart> findByPartCatalogId(Long partCatalogId);

    /**
     * Sum of what the used parts cost the shop (purchase price) across the given repairs.
     * Parts with a null purchase price contribute 0.
     */
    @Query("SELECT COALESCE(SUM(rp.quantity * pc.purchasePrice), 0) FROM RepairPart rp " +
           "JOIN rp.partCatalog pc WHERE rp.repair.id IN :repairIds")
    double sumPurchaseCostByRepairIds(@Param("repairIds") List<Long> repairIds);

}
