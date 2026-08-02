package com.smartfixsamana.models.services;

import com.smartfixsamana.models.dto.DailyEarningsResponse;
import com.smartfixsamana.models.dto.EarningsSummaryResponse;
import com.smartfixsamana.models.dto.RangeEarningsResponse;
import com.smartfixsamana.models.entities.ExternalRepair;
import com.smartfixsamana.models.entities.Repair;
import com.smartfixsamana.models.enums.ExternalRepairStatus;
import com.smartfixsamana.models.repositories.IExternalRepairRepository;
import com.smartfixsamana.models.repositories.IRepairPartRepository;
import com.smartfixsamana.models.repositories.IRepairRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de cálculo de ganancias.
 *
 * <p><b>Reparaciones propias:</b> ganancia = mano de obra + margen de repuestos
 * ({@code precio de venta - precio de compra}). Equivale a {@code totalCost - costo de compra de repuestos},
 * ya que {@code totalCost} ya incluye repuestos (a precio de venta) más mano de obra.</p>
 * <p><b>Reparaciones externas:</b> la ganancia del taller es únicamente el 60&nbsp;% de la utilidad
 * neta ({@code netProfit * 0.60}), <b>sin</b> el costo del repuesto: ese valor es recuperación de
 * un gasto, no ganancia. Solo se incluyen las reparaciones con estado {@code REPARADO} o
 * {@code ENTREGADO}.</p>
 * <p>{@code ExternalRepair.getMyShare()} sigue existiendo sin cambios —devuelve
 * {@code (netProfit * 0.60) + partCost}— porque el módulo de reparaciones externas y las
 * liquidaciones sí necesitan incluir la recuperación del repuesto; este servicio no lo usa.</p>
 */
@Service
public class EarningsService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");
    private static final double MY_SHARE_RATE = 0.60;

    private final IRepairRepository repairRepository;
    private final IExternalRepairRepository externalRepairRepository;
    private final IRepairPartRepository repairPartRepository;

    public EarningsService(IRepairRepository repairRepository,
                           IExternalRepairRepository externalRepairRepository,
                           IRepairPartRepository repairPartRepository) {
        this.repairRepository = repairRepository;
        this.externalRepairRepository = externalRepairRepository;
        this.repairPartRepository = repairPartRepository;
    }

    public DailyEarningsResponse getDailyEarnings(LocalDate date) {
        List<Repair> repairs = repairRepository.findByDateBetween(date, date);
        List<ExternalRepair> externalRepairs = getFilteredExternalRepairs(date, date);

        double ownTotal = round(computeOwnEarnings(repairs));
        double externalTotal = round(externalRepairs.stream()
                .mapToDouble(this::computeExternalShare)
                .sum());

        return new DailyEarningsResponse(
                date,
                ownTotal,
                externalTotal,
                round(ownTotal + externalTotal),
                repairs.size(),
                externalRepairs.size()
        );
    }

    public RangeEarningsResponse getRangeEarnings(LocalDate startDate, LocalDate endDate) {
        List<Repair> repairs = repairRepository.findByDateBetween(startDate, endDate);
        List<ExternalRepair> externalRepairs = getFilteredExternalRepairs(startDate, endDate);

        double ownTotal = round(computeOwnEarnings(repairs));
        double externalTotal = round(externalRepairs.stream()
                .mapToDouble(this::computeExternalShare)
                .sum());

        return new RangeEarningsResponse(
                startDate,
                endDate,
                ownTotal,
                externalTotal,
                round(ownTotal + externalTotal),
                repairs.size(),
                externalRepairs.size()
        );
    }

    public EarningsSummaryResponse getSummary() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        DailyEarningsResponse todayEarnings = getDailyEarnings(today);
        RangeEarningsResponse last30Days = getRangeEarnings(today.minusDays(30), today);
        RangeEarningsResponse currentMonth = getRangeEarnings(
                today.withDayOfMonth(1), today);

        return new EarningsSummaryResponse(todayEarnings, last30Days, currentMonth);
    }

    /**
     * Own-repair profit = labor + parts margin.
     * {@code totalCost} already equals (parts at sale price + labor), so subtracting what the
     * parts cost the shop (purchase price) leaves labor plus the markup made on the parts.
     */
    private double computeOwnEarnings(List<Repair> repairs) {
        if (repairs.isEmpty()) {
            return 0.0;
        }
        double revenue = repairs.stream()
                .mapToDouble(r -> safe(r.getTotalCost()))
                .sum();
        List<Long> repairIds = repairs.stream().map(Repair::getId).toList();
        double partsPurchaseCost = repairPartRepository.sumPurchaseCostByRepairIds(repairIds);
        return revenue - partsPurchaseCost;
    }

    /**
     * Ganancia real del taller por una reparación externa dentro del módulo de Ganancias:
     * solo el 60 % de la utilidad neta. NO incluye el costo del repuesto, porque ese valor
     * es recuperación de un gasto, no ganancia.
     */
    private double computeExternalShare(ExternalRepair repair) {
        return safe(repair.getNetProfit()) * MY_SHARE_RATE;
    }

    /**
     * Returns external repairs filtered by date range and included statuses (REPARADO, ENTREGADO).
     */
    private List<ExternalRepair> getFilteredExternalRepairs(LocalDate start, LocalDate end) {
        List<ExternalRepair> result = new ArrayList<>();
        result.addAll(externalRepairRepository.findByStatusAndDateBetween(
                ExternalRepairStatus.REPARADO, start, end));
        result.addAll(externalRepairRepository.findByStatusAndDateBetween(
                ExternalRepairStatus.ENTREGADO, start, end));
        return result;
    }

    private double safe(Double value) {
        return value != null ? value : 0.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

}
