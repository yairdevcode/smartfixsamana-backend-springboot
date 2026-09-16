package com.smartfixsamana.models.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartfixsamana.models.entities.ExternalRepair;
import com.smartfixsamana.models.entities.Settlement;
import com.smartfixsamana.models.enums.ExternalRepairStatus;
import com.smartfixsamana.models.enums.SettlementStatus;
import com.smartfixsamana.models.repositories.IExternalRepairRepository;
import com.smartfixsamana.models.repositories.ISettlementRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Casos LIQ-001 y LIQ-003 del plan de pruebas GA9-220501096-AA1-EV02.
 *
 * Verifica que la liquidación de un periodo consolide correctamente los valores
 * de las reparaciones externas, que arrastre las pendientes de recoger de
 * periodos anteriores y que un periodo sin reparaciones no genere liquidación.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementService - consolidación de liquidaciones")
class SettlementServiceTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 8, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 8, 31);

    @Mock
    private ISettlementRepository settlementRepository;

    @Mock
    private IExternalRepairRepository externalRepairRepository;

    private SettlementService service;

    @BeforeEach
    void setUp() {
        service = new SettlementService(settlementRepository, externalRepairRepository);
    }

    /**
     * Reparación externa con precio y costo de repuesto.
     * La utilidad neta es el precio menos el costo; de ella el técnico toma el
     * 60 % más el costo del repuesto, y el local el 40 % restante.
     */
    private ExternalRepair reparacion(long id, double precio, double costoRepuesto) {
        ExternalRepair reparacion = new ExternalRepair();
        reparacion.setId(id);
        reparacion.setClientName("Cliente " + id);
        reparacion.setRepairPrice(precio);
        reparacion.setPartCost(costoRepuesto);
        reparacion.setStatus(ExternalRepairStatus.REPARADO);
        reparacion.setDate(LocalDate.of(2026, 8, 15));
        return reparacion;
    }

    private void devolverLaLiquidacionGuardada() {
        when(settlementRepository.save(any(Settlement.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private void sinPendientesDePeriodosAnteriores() {
        when(externalRepairRepository.findByStatusAndSettlementIsNotNull(
                ExternalRepairStatus.PENDIENTE_RECOGER)).thenReturn(List.of());
    }

    private void reparacionesDelPeriodo(List<ExternalRepair> reparaciones) {
        when(externalRepairRepository.findBySettlementIsNullAndStatusAndDateBetween(
                ExternalRepairStatus.REPARADO, INICIO, FIN)).thenReturn(reparaciones);
    }

    // ---------------------------------------------------------------- LIQ-001

    @Test
    @DisplayName("LIQ-001: la liquidación suma los valores de todas las reparaciones del periodo")
    void laLiquidacionSumaLosValoresDelPeriodo() {
        // 200000 - 50000 = 150000 neto -> 90000 + 50000 = 140000 técnico / 60000 local
        // 100000 -     0 = 100000 neto -> 60000         =  60000 técnico / 40000 local
        reparacionesDelPeriodo(List.of(reparacion(1L, 200000.0, 50000.0),
                                       reparacion(2L, 100000.0, 0.0)));
        sinPendientesDePeriodosAnteriores();
        devolverLaLiquidacionGuardada();

        service.createSettlement(INICIO, FIN);

        ArgumentCaptor<Settlement> capturada = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(capturada.capture());
        Settlement liquidacion = capturada.getValue();

        assertThat(liquidacion.getTotalRepairPrice()).isEqualTo(300000.0);
        assertThat(liquidacion.getTotalPartCost()).isEqualTo(50000.0);
        assertThat(liquidacion.getTotalMyShare()).isEqualTo(200000.0);
        assertThat(liquidacion.getTotalStoreShare()).isEqualTo(100000.0);
    }

    @Test
    @DisplayName("LIQ-001: la liquidación queda con las fechas del periodo y estado LIQUIDADA")
    void laLiquidacionRegistraElPeriodoYQuedaLiquidada() {
        reparacionesDelPeriodo(List.of(reparacion(1L, 150000.0, 30000.0)));
        sinPendientesDePeriodosAnteriores();
        devolverLaLiquidacionGuardada();

        service.createSettlement(INICIO, FIN);

        ArgumentCaptor<Settlement> capturada = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(capturada.capture());
        Settlement liquidacion = capturada.getValue();

        assertThat(liquidacion.getStartDate()).isEqualTo(INICIO);
        assertThat(liquidacion.getEndDate()).isEqualTo(FIN);
        assertThat(liquidacion.getStatus()).isEqualTo(SettlementStatus.LIQUIDADA);
    }

    @Test
    @DisplayName("LIQ-001: cada reparación liquidada queda asociada a la liquidación creada")
    void cadaReparacionQuedaAsociadaALaLiquidacion() {
        ExternalRepair primera = reparacion(1L, 200000.0, 50000.0);
        ExternalRepair segunda = reparacion(2L, 100000.0, 0.0);
        reparacionesDelPeriodo(List.of(primera, segunda));
        sinPendientesDePeriodosAnteriores();
        devolverLaLiquidacionGuardada();

        service.createSettlement(INICIO, FIN);

        verify(externalRepairRepository).saveAll(any());
        assertThat(primera.getSettlement()).isNotNull();
        assertThat(segunda.getSettlement()).isNotNull();
        assertThat(primera.getSettlement()).isSameAs(segunda.getSettlement());
    }

    @Test
    @DisplayName("LIQ-001: un costo de repuesto nulo se cuenta como cero y no rompe el total")
    void unCostoDeRepuestoNuloSeCuentaComoCero() {
        ExternalRepair sinCosto = reparacion(1L, 120000.0, 0.0);
        sinCosto.setPartCost(null);
        reparacionesDelPeriodo(List.of(sinCosto));
        sinPendientesDePeriodosAnteriores();
        devolverLaLiquidacionGuardada();

        service.createSettlement(INICIO, FIN);

        ArgumentCaptor<Settlement> capturada = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(capturada.capture());

        assertThat(capturada.getValue().getTotalPartCost()).isZero();
        assertThat(capturada.getValue().getTotalMyShare()).isEqualTo(72000.0);
        assertThat(capturada.getValue().getTotalStoreShare()).isEqualTo(48000.0);
    }

    @Test
    @DisplayName("LIQ-001: sin arrastres la liquidación no incluye advertencia")
    void sinArrastresNoHayAdvertencia() {
        reparacionesDelPeriodo(List.of(reparacion(1L, 150000.0, 30000.0)));
        sinPendientesDePeriodosAnteriores();
        devolverLaLiquidacionGuardada();

        Map<String, Object> resultado = service.createSettlement(INICIO, FIN);

        assertThat(resultado).containsKey("settlement");
        assertThat(resultado).doesNotContainKey("warning");
    }

    // --------------------------------------------------- arrastre de periodos

    @Test
    @DisplayName("LIQ-002: las pendientes de recoger de periodos anteriores se arrastran")
    void lasPendientesDeRecogerSeArrastran() {
        ExternalRepair arrastrada = reparacion(9L, 80000.0, 20000.0);
        arrastrada.setStatus(ExternalRepairStatus.PENDIENTE_RECOGER);

        reparacionesDelPeriodo(List.of(reparacion(1L, 200000.0, 50000.0)));
        when(externalRepairRepository.findByStatusAndSettlementIsNotNull(
                ExternalRepairStatus.PENDIENTE_RECOGER)).thenReturn(List.of(arrastrada));
        devolverLaLiquidacionGuardada();

        service.createSettlement(INICIO, FIN);

        ArgumentCaptor<Settlement> capturada = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(capturada.capture());

        assertThat(capturada.getValue().getTotalRepairPrice()).isEqualTo(280000.0);
    }

    @Test
    @DisplayName("LIQ-002: si solo hay arrastres la liquidación advierte que no hubo reparaciones nuevas")
    void siSoloHayArrastresSeAdvierte() {
        ExternalRepair arrastrada = reparacion(9L, 80000.0, 20000.0);
        arrastrada.setStatus(ExternalRepairStatus.PENDIENTE_RECOGER);

        reparacionesDelPeriodo(List.of());
        when(externalRepairRepository.findByStatusAndSettlementIsNotNull(
                ExternalRepairStatus.PENDIENTE_RECOGER)).thenReturn(List.of(arrastrada));
        devolverLaLiquidacionGuardada();

        Map<String, Object> resultado = service.createSettlement(INICIO, FIN);

        assertThat(resultado).containsKey("warning");
        assertThat(resultado.get("warning").toString())
                .contains("No se encontraron reparaciones nuevas");
    }

    // ---------------------------------------------------------------- LIQ-003

    @Test
    @DisplayName("LIQ-003: un periodo sin reparaciones no genera liquidación")
    void unPeriodoSinReparacionesNoGeneraLiquidacion() {
        reparacionesDelPeriodo(List.of());
        sinPendientesDePeriodosAnteriores();

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> service.createSettlement(INICIO, FIN))
                .withMessageContaining("No hay reparaciones disponibles para liquidar");

        verify(settlementRepository, never()).save(any(Settlement.class));
        verify(externalRepairRepository, never()).saveAll(any());
    }
}
