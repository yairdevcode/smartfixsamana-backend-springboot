package com.smartfixsamana.models.services;

import com.smartfixsamana.models.entities.ExternalRepair;
import com.smartfixsamana.models.entities.Settlement;
import com.smartfixsamana.models.enums.SettlementStatus;
import com.smartfixsamana.models.repositories.IExternalRepairRepository;
import com.smartfixsamana.models.repositories.ISettlementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfixsamana.models.enums.ExternalRepairStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SettlementService {

    private final ISettlementRepository settlementRepository;
    private final IExternalRepairRepository externalRepairRepository;

    public SettlementService(ISettlementRepository settlementRepository,
                             IExternalRepairRepository externalRepairRepository) {
        this.settlementRepository = settlementRepository;
        this.externalRepairRepository = externalRepairRepository;
    }

    public List<Settlement> findAll() {
        return settlementRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public Optional<Settlement> findById(Long id) {
        return settlementRepository.findById(id);
    }

    public Page<Settlement> findAllPaginated(int page, int size) {
        return settlementRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /**
     * Creates a settlement. Returns a map with "settlement" and optionally "warning".
     */
    @Transactional
    public Map<String, Object> createSettlement(LocalDate startDate, LocalDate endDate) {
        // 1. New unsettled REPARADO repairs within the date range
        List<ExternalRepair> newRepairs = externalRepairRepository
                .findBySettlementIsNullAndStatusAndDateBetween(
                        ExternalRepairStatus.REPARADO, startDate, endDate);

        // 2. Carried-over repairs: PENDIENTE_RECOGER from any previous settlement
        List<ExternalRepair> carriedOverRepairs = externalRepairRepository
                .findByStatusAndSettlementIsNotNull(ExternalRepairStatus.PENDIENTE_RECOGER);

        // Combine both lists
        List<ExternalRepair> allRepairs = new ArrayList<>(newRepairs);
        allRepairs.addAll(carriedOverRepairs);

        if (allRepairs.isEmpty()) {
            throw new RuntimeException("No hay reparaciones disponibles para liquidar.");
        }

        String warning = null;
        if (newRepairs.isEmpty() && !carriedOverRepairs.isEmpty()) {
            warning = "No se encontraron reparaciones nuevas en este rango de fechas. "
                    + "La liquidación solo incluye " + carriedOverRepairs.size()
                    + " reparación(es) pendiente(s) de recoger de períodos anteriores.";
        }

        double totalRepairPrice = 0;
        double totalPartCost = 0;
        double totalMyShare = 0;
        double totalStoreShare = 0;

        for (ExternalRepair repair : allRepairs) {
            totalRepairPrice += repair.getRepairPrice();
            totalPartCost += (repair.getPartCost() != null ? repair.getPartCost() : 0.0);
            totalMyShare += repair.getMyShare();
            totalStoreShare += repair.getStoreShare();
        }

        Settlement settlement = new Settlement();
        settlement.setStartDate(startDate);
        settlement.setEndDate(endDate);
        settlement.setTotalRepairPrice(totalRepairPrice);
        settlement.setTotalPartCost(totalPartCost);
        settlement.setTotalMyShare(totalMyShare);
        settlement.setTotalStoreShare(totalStoreShare);
        settlement.setStatus(SettlementStatus.LIQUIDADA);

        Settlement saved = settlementRepository.save(settlement);

        for (ExternalRepair repair : allRepairs) {
            repair.setSettlement(saved);
        }
        externalRepairRepository.saveAll(allRepairs);

        saved.setRepairs(allRepairs);

        Map<String, Object> result = new HashMap<>();
        result.put("settlement", saved);
        if (warning != null) {
            result.put("warning", warning);
        }
        return result;
    }
}
