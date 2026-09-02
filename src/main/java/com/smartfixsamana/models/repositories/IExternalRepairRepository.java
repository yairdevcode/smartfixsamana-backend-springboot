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

    /**
     * Paginated listing with optional status, date range and keyword filters.
     *
     * <p>The keyword is matched against the client name and the phone brand/model joined into a
     * single "clientName phoneBrand" haystack, so a multi-word keyword spanning both fields,
     * such as "perez moto" for "Rodrigo Perez" / "Moto G04", matches too. Like every LIKE
     * search here it matches a contiguous substring, so words that skip over part of the name
     * ("rodrigo moto") do not match. Both fields are wrapped in COALESCE because MySQL's
     * CONCAT returns NULL as soon as one argument is NULL, the same trap documented on
     * {@link IPartCatalogRepository#searchAvailableParts}.
     */
    @Query("SELECT e FROM ExternalRepair e WHERE " +
            "(:status IS NULL OR e.status = :status) AND " +
            "(:startDate IS NULL OR e.date >= :startDate) AND " +
            "(:endDate IS NULL OR e.date <= :endDate) AND " +
            "(:keyword IS NULL OR LOWER(CONCAT(COALESCE(e.clientName, ''), ' ', " +
            "COALESCE(e.phoneBrand, ''))) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ExternalRepair> findWithFilters(
            @Param("status") ExternalRepairStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
