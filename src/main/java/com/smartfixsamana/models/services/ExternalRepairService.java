package com.smartfixsamana.models.services;

import com.smartfixsamana.models.dto.ExternalRepairDTO;
import com.smartfixsamana.models.dto.ExternalRepairResponse;
import com.smartfixsamana.models.dto.ImportReconciliationResponse;
import com.smartfixsamana.models.entities.ExternalRepair;
import com.smartfixsamana.models.enums.ExternalRepairStatus;
import com.smartfixsamana.models.repositories.IExternalRepairRepository;
import com.smartfixsamana.models.services.ExternalRepairExcelService.ExcelImportRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExternalRepairService {

    private final IExternalRepairRepository repository;

    public ExternalRepairService(IExternalRepairRepository repository) {
        this.repository = repository;
    }

    public Page<ExternalRepair> findAllPaginated(int page, int size, String sortBy, String sortDirection,
                                                  ExternalRepairStatus status, LocalDate startDate, LocalDate endDate) {
        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return repository.findWithFilters(status, startDate, endDate, pageable);
    }

    public Optional<ExternalRepair> findById(Long id) {
        return repository.findById(id);
    }

    public ExternalRepair save(ExternalRepairDTO dto) {
        ExternalRepair entity = new ExternalRepair();
        updateFromDTO(entity, dto);
        return repository.save(entity);
    }

    public ExternalRepair update(Long id, ExternalRepairDTO dto) {
        ExternalRepair entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("External repair not found with id: " + id));
        updateFromDTO(entity, dto);
        return repository.save(entity);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public List<ExternalRepair> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return repository.findByDateBetween(startDate, endDate);
    }

    public List<ExternalRepair> findUnsettledByDateRange(LocalDate startDate, LocalDate endDate) {
        return repository.findBySettlementIsNullAndDateBetween(startDate, endDate);
    }

    public List<ExternalRepair> findByStatusAndDateRange(ExternalRepairStatus status, LocalDate startDate, LocalDate endDate) {
        return repository.findByStatusAndDateBetween(status, startDate, endDate);
    }

    /**
     * Preview reconciliation: given the imported Excel rows and the current repairs in the filtered date range,
     * determine which repairs are ENTREGADO (not in Excel) and which are PENDIENTE_RECOGER (still in Excel).
     */
    public ImportReconciliationResponse previewReconciliation(List<ExcelImportRow> importedRows,
                                                               LocalDate startDate, LocalDate endDate) {
        List<ExternalRepair> currentRepairs = repository.findByDateBetween(startDate, endDate);

        // Build a set of matching keys from the imported Excel
        Set<String> importedKeys = importedRows.stream()
                .map(row -> buildMatchKey(row.clientName(), row.phoneBrand(), row.solution(), row.repairPrice()))
                .collect(Collectors.toSet());

        List<ExternalRepairResponse> entregadas = new ArrayList<>();
        List<ExternalRepairResponse> pendientesRecoger = new ArrayList<>();

        for (ExternalRepair repair : currentRepairs) {
            String key = buildMatchKey(repair.getClientName(), repair.getPhoneBrand(),
                    repair.getSolution(), repair.getRepairPrice());
            if (importedKeys.contains(key)) {
                pendientesRecoger.add(ExternalRepairResponse.fromEntity(repair));
            } else {
                entregadas.add(ExternalRepairResponse.fromEntity(repair));
            }
        }

        return new ImportReconciliationResponse(entregadas, pendientesRecoger,
                entregadas.size(), pendientesRecoger.size());
    }

    /**
     * Apply the reconciliation: mark matched repairs as PENDIENTE_RECOGER, unmatched as ENTREGADO.
     */
    @Transactional
    public ImportReconciliationResponse applyReconciliation(List<ExcelImportRow> importedRows,
                                                             LocalDate startDate, LocalDate endDate) {
        List<ExternalRepair> currentRepairs = repository.findByDateBetween(startDate, endDate);

        Set<String> importedKeys = importedRows.stream()
                .map(row -> buildMatchKey(row.clientName(), row.phoneBrand(), row.solution(), row.repairPrice()))
                .collect(Collectors.toSet());

        List<ExternalRepairResponse> entregadas = new ArrayList<>();
        List<ExternalRepairResponse> pendientesRecoger = new ArrayList<>();

        for (ExternalRepair repair : currentRepairs) {
            String key = buildMatchKey(repair.getClientName(), repair.getPhoneBrand(),
                    repair.getSolution(), repair.getRepairPrice());
            if (importedKeys.contains(key)) {
                repair.setStatus(ExternalRepairStatus.PENDIENTE_RECOGER);
                pendientesRecoger.add(ExternalRepairResponse.fromEntity(repair));
            } else {
                repair.setStatus(ExternalRepairStatus.ENTREGADO);
                entregadas.add(ExternalRepairResponse.fromEntity(repair));
            }
        }

        repository.saveAll(currentRepairs);

        return new ImportReconciliationResponse(entregadas, pendientesRecoger,
                entregadas.size(), pendientesRecoger.size());
    }

    /**
     * Preview reconciliation for a specific settlement: matches imported Excel rows against ALL repairs
     * in the settlement (both new repairs and carried-over PENDIENTE_RECOGER from previous periods).
     */
    public ImportReconciliationResponse previewReconciliationBySettlement(
            List<ExcelImportRow> importedRows, Long settlementId, LocalDate settlementStartDate) {
        List<ExternalRepair> settlementRepairs = repository.findBySettlementId(settlementId);
        return reconcileAgainstRepairs(importedRows, settlementRepairs, settlementStartDate, false);
    }

    /**
     * Apply reconciliation for a specific settlement: marks matched repairs as PENDIENTE_RECOGER,
     * unmatched as ENTREGADO. Works against ALL repairs in the settlement.
     */
    @Transactional
    public ImportReconciliationResponse applyReconciliationBySettlement(
            List<ExcelImportRow> importedRows, Long settlementId, LocalDate settlementStartDate) {
        List<ExternalRepair> settlementRepairs = repository.findBySettlementId(settlementId);
        return reconcileAgainstRepairs(importedRows, settlementRepairs, settlementStartDate, true);
    }

    private ImportReconciliationResponse reconcileAgainstRepairs(
            List<ExcelImportRow> importedRows, List<ExternalRepair> repairs,
            LocalDate settlementStartDate, boolean apply) {

        Set<String> importedKeys = importedRows.stream()
                .map(row -> buildMatchKey(row.clientName(), row.phoneBrand(), row.solution(), row.repairPrice()))
                .collect(Collectors.toSet());

        List<ExternalRepairResponse> entregadas = new ArrayList<>();
        List<ExternalRepairResponse> pendientesRecoger = new ArrayList<>();
        int entregadasNuevas = 0;
        int entregadasPendientesAnteriores = 0;

        for (ExternalRepair repair : repairs) {
            String key = buildMatchKey(repair.getClientName(), repair.getPhoneBrand(),
                    repair.getSolution(), repair.getRepairPrice());
            boolean isCarriedOver = repair.getDate().isBefore(settlementStartDate);

            if (importedKeys.contains(key)) {
                if (apply) {
                    repair.setStatus(ExternalRepairStatus.PENDIENTE_RECOGER);
                }
                pendientesRecoger.add(ExternalRepairResponse.fromEntity(repair));
            } else {
                if (apply) {
                    repair.setStatus(ExternalRepairStatus.ENTREGADO);
                }
                entregadas.add(ExternalRepairResponse.fromEntity(repair));
                if (isCarriedOver) {
                    entregadasPendientesAnteriores++;
                } else {
                    entregadasNuevas++;
                }
            }
        }

        if (apply) {
            repository.saveAll(repairs);
        }

        return new ImportReconciliationResponse(entregadas, pendientesRecoger,
                entregadas.size(), pendientesRecoger.size(),
                entregadasNuevas, entregadasPendientesAnteriores);
    }

    private String buildMatchKey(String clientName, String phoneBrand, String solution, Double repairPrice) {
        return (clientName != null ? clientName.trim().toLowerCase() : "") + "|" +
                (phoneBrand != null ? phoneBrand.trim().toLowerCase() : "") + "|" +
                (solution != null ? solution.trim().toLowerCase() : "") + "|" +
                (repairPrice != null ? String.format("%.2f", repairPrice) : "0.00");
    }

    private void updateFromDTO(ExternalRepair entity, ExternalRepairDTO dto) {
        entity.setClientName(dto.clientName());
        entity.setPhoneBrand(dto.phoneBrand());
        entity.setSolution(dto.solution());
        entity.setRepairPrice(dto.repairPrice());
        entity.setPartCost(dto.partCost() != null ? dto.partCost() : 0.0);
        entity.setStatus(dto.status());
        entity.setDate(dto.date());
        entity.setNotes(dto.notes());
    }
}
