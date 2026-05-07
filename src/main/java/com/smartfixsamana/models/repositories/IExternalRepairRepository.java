package com.smartfixsamana.models.repositories;

import com.smartfixsamana.models.entities.ExternalRepair;
import com.smartfixsamana.models.enums.ExternalRepairStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface IExternalRepairRepository extends JpaRepository<ExternalRepair, Long> {

    Page<ExternalRepair> findByStatus(ExternalRepairStatus status, Pageable pageable);

    List<ExternalRepair> findByDateBetween(LocalDate start, LocalDate end);

    List<ExternalRepair> findByStatusAndDateBetween(ExternalRepairStatus status, LocalDate start, LocalDate end);

    List<ExternalRepair> findBySettlementIsNullAndDateBetween(LocalDate start, LocalDate end);

    List<ExternalRepair> findByStatusAndSettlementIsNotNull(ExternalRepairStatus status);

    List<ExternalRepair> findBySettlementIsNullAndStatusAndDateBetween(
            ExternalRepairStatus status, LocalDate start, LocalDate end);

    List<ExternalRepair> findBySettlementId(Long settlementId);

    @Query("SELECT e FROM ExternalRepair e WHERE " +
            "(:status IS NULL OR e.status = :status) AND " +
            "(:startDate IS NULL OR e.date >= :startDate) AND " +
            "(:endDate IS NULL OR e.date <= :endDate)")
    Page<ExternalRepair> findWithFilters(
            @Param("status") ExternalRepairStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );
}
