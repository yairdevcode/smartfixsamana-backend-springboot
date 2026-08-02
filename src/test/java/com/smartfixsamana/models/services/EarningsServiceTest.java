package com.smartfixsamana.models.services;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartfixsamana.models.dto.DailyEarningsResponse;
import com.smartfixsamana.models.dto.RangeEarningsResponse;
import com.smartfixsamana.models.entities.ExternalRepair;
import com.smartfixsamana.models.entities.Repair;
import com.smartfixsamana.models.enums.ExternalRepairStatus;
import com.smartfixsamana.models.repositories.IExternalRepairRepository;
import com.smartfixsamana.models.repositories.IRepairPartRepository;
import com.smartfixsamana.models.repositories.IRepairRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EarningsServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 2, 10);

    @Mock
    private IRepairRepository repairRepository;

    @Mock
    private IExternalRepairRepository externalRepairRepository;

    @Mock
    private IRepairPartRepository repairPartRepository;

    private EarningsService service;

    @BeforeEach
    void setUp() {
        service = new EarningsService(repairRepository, externalRepairRepository, repairPartRepository);

        // Nothing exists unless a test stubs it.
        lenient().when(repairRepository.findByDateBetween(DATE, DATE)).thenReturn(List.of());
        lenient().when(externalRepairRepository.findByStatusAndDateBetween(
                ExternalRepairStatus.REPARADO, DATE, DATE)).thenReturn(List.of());
        lenient().when(externalRepairRepository.findByStatusAndDateBetween(
                ExternalRepairStatus.ENTREGADO, DATE, DATE)).thenReturn(List.of());
        lenient().when(repairPartRepository.sumPurchaseCostByRepairIds(anyList())).thenReturn(0.0);
    }

    @Test
    void countsOnlySixtyPercentOfNetProfitForExternalRepairs() {
        // netProfit = 100000 - 40000 = 60000 -> 36000. The 40000 part cost is expense recovery,
        // not profit, so it must NOT be added (that would give 76000).
        givenRepairedExternalRepairs(externalRepair(100000.0, 40000.0));

        DailyEarningsResponse earnings = service.getDailyEarnings(DATE);

        assertThat(earnings.externalRepairsMyShare()).isEqualTo(36000.0);
        assertThat(earnings.externalRepairsCount()).isEqualTo(1);
    }

    @Test
    void countsSixtyPercentWhenThereIsNoPartCost() {
        givenRepairedExternalRepairs(externalRepair(50000.0, 0.0));

        DailyEarningsResponse earnings = service.getDailyEarnings(DATE);

        assertThat(earnings.externalRepairsMyShare()).isEqualTo(30000.0);
    }

    @Test
    void treatsNullPartCostAsZero() {
        givenRepairedExternalRepairs(externalRepair(50000.0, null));

        assertThatCode(() -> service.getDailyEarnings(DATE)).doesNotThrowAnyException();
        assertThat(service.getDailyEarnings(DATE).externalRepairsMyShare()).isEqualTo(30000.0);
    }

    @Test
    void totalIsOwnEarningsPlusSixtyPercentOfExternalNetProfit() {
        // Own repair: totalCost 80000 with 30000 of parts bought -> 50000 of profit.
        Repair repair = new Repair();
        repair.setId(7L);
        repair.setTotalCost(80000.0);
        when(repairRepository.findByDateBetween(DATE, DATE)).thenReturn(List.of(repair));
        when(repairPartRepository.sumPurchaseCostByRepairIds(List.of(7L))).thenReturn(30000.0);

        givenRepairedExternalRepairs(externalRepair(100000.0, 40000.0));

        DailyEarningsResponse earnings = service.getDailyEarnings(DATE);

        assertThat(earnings.ownRepairsTotal()).isEqualTo(50000.0);
        assertThat(earnings.externalRepairsMyShare()).isEqualTo(36000.0);
        assertThat(earnings.total()).isEqualTo(86000.0);
        assertThat(earnings.ownRepairsCount()).isEqualTo(1);
        assertThat(earnings.externalRepairsCount()).isEqualTo(1);
    }

    @Test
    void includesBothRepairedAndDeliveredExternalRepairs() {
        when(externalRepairRepository.findByStatusAndDateBetween(
                ExternalRepairStatus.REPARADO, DATE, DATE))
                .thenReturn(List.of(externalRepair(100000.0, 40000.0)));
        when(externalRepairRepository.findByStatusAndDateBetween(
                ExternalRepairStatus.ENTREGADO, DATE, DATE))
                .thenReturn(List.of(externalRepair(50000.0, 0.0)));

        DailyEarningsResponse earnings = service.getDailyEarnings(DATE);

        assertThat(earnings.externalRepairsMyShare()).isEqualTo(66000.0);
        assertThat(earnings.externalRepairsCount()).isEqualTo(2);
    }

    @Test
    void rangeEarningsAlsoCountOnlyTheSixtyPercentShare() {
        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = LocalDate.of(2026, 2, 28);
        when(repairRepository.findByDateBetween(start, end)).thenReturn(List.of());
        when(externalRepairRepository.findByStatusAndDateBetween(
                ExternalRepairStatus.REPARADO, start, end))
                .thenReturn(List.of(externalRepair(100000.0, 40000.0)));
        when(externalRepairRepository.findByStatusAndDateBetween(
                ExternalRepairStatus.ENTREGADO, start, end)).thenReturn(List.of());

        RangeEarningsResponse earnings = service.getRangeEarnings(start, end);

        assertThat(earnings.externalRepairsMyShare()).isEqualTo(36000.0);
        assertThat(earnings.total()).isEqualTo(36000.0);
    }

    @Test
    void leavesGetMyShareUntouchedForTheExternalRepairsModule() {
        // The entity still reports share + part cost; only Earnings drops the part cost.
        ExternalRepair repair = externalRepair(100000.0, 40000.0);

        assertThat(repair.getMyShare()).isEqualTo(76000.0);
        assertThat(repair.getNetProfit()).isEqualTo(60000.0);
        assertThat(repair.getStoreShare()).isEqualTo(24000.0);
    }

    private void givenRepairedExternalRepairs(ExternalRepair... repairs) {
        when(externalRepairRepository.findByStatusAndDateBetween(
                ExternalRepairStatus.REPARADO, DATE, DATE)).thenReturn(List.of(repairs));
    }

    private ExternalRepair externalRepair(Double repairPrice, Double partCost) {
        ExternalRepair repair = new ExternalRepair();
        repair.setRepairPrice(repairPrice);
        repair.setPartCost(partCost);
        repair.setDate(DATE);
        repair.setStatus(ExternalRepairStatus.REPARADO);
        return repair;
    }
}
