package com.smartfixsamana.models.services;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.smartfixsamana.models.dto.RepairPartRequest;
import com.smartfixsamana.models.entities.PartCatalog;
import com.smartfixsamana.models.entities.Repair;
import com.smartfixsamana.models.entities.RepairPart;
import com.smartfixsamana.models.repositories.IRepairPartRepository;
import com.smartfixsamana.models.repositories.IRepairRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Casos REP-002, REP-005 y REP-007 del plan de pruebas GA9-220501096-AA1-EV02.
 *
 * Verifica el cálculo del costo total de una reparación y el efecto que la
 * adición y el retiro de repuestos producen sobre las existencias del catálogo.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RepairPartService - costo de reparación y movimiento de repuestos")
class RepairPartServiceTest {

    private static final Long REPAIR_ID = 100L;
    private static final Long PART_ID = 1L;
    private static final double MANO_DE_OBRA = 30000.0;
    private static final double PRECIO_VENTA = 140000.0;

    @Mock
    private IRepairPartRepository repairPartRepository;

    @Mock
    private IRepairRepository repairRepository;

    @Mock
    private PartCatalogService partCatalogService;

    @Mock
    private InventoryMovementService inventoryMovementService;

    private RepairPartService service;
    private Repair reparacion;
    private PartCatalog repuesto;

    @BeforeEach
    void setUp() {
        service = new RepairPartService(repairPartRepository, repairRepository,
                partCatalogService, inventoryMovementService);

        reparacion = new Repair();
        reparacion.setId(REPAIR_ID);
        reparacion.setState("EN_PROCESO");
        reparacion.setLaborCost(MANO_DE_OBRA);

        repuesto = new PartCatalog();
        repuesto.setId(PART_ID);
        repuesto.setName("Pantalla Samsung A15");
        repuesto.setQuantity(10);
        repuesto.setPurchasePrice(85000.0);
        repuesto.setSalePrice(PRECIO_VENTA);
    }

    private void devolverElRepuestoGuardado() {
        when(repairPartRepository.save(any(RepairPart.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private RepairPart repuestoDeReparacion(int cantidad, double precioCobrado) {
        RepairPart parte = new RepairPart();
        parte.setId(500L);
        parte.setRepair(reparacion);
        parte.setPartCatalog(repuesto);
        parte.setQuantity(cantidad);
        parte.setPriceCharged(precioCobrado);
        return parte;
    }

    // ---------------------------------------------------------------- REP-005

    @Test
    @DisplayName("REP-005: agregar un repuesto genera el movimiento de consumo del inventario")
    void agregarUnRepuestoGeneraElMovimientoDeConsumo() {
        when(repairRepository.findById(REPAIR_ID)).thenReturn(Optional.of(reparacion));
        when(partCatalogService.findById(PART_ID)).thenReturn(Optional.of(repuesto));
        when(repairPartRepository.findByRepairId(REPAIR_ID)).thenReturn(List.of());
        devolverElRepuestoGuardado();

        service.addPartToRepair(REPAIR_ID, new RepairPartRequest(PART_ID, 2, PRECIO_VENTA));

        verify(inventoryMovementService).createRepairUseMovement(
                eq(repuesto), eq(reparacion), eq(2), any(String.class));
    }

    @Test
    @DisplayName("REP-005: si no se envía precio se toma el precio de venta del catálogo")
    void siNoSeEnviaPrecioSeTomaElDelCatalogo() {
        when(repairRepository.findById(REPAIR_ID)).thenReturn(Optional.of(reparacion));
        when(partCatalogService.findById(PART_ID)).thenReturn(Optional.of(repuesto));
        when(repairPartRepository.findByRepairId(REPAIR_ID)).thenReturn(List.of());
        devolverElRepuestoGuardado();

        RepairPart guardado = service.addPartToRepair(
                REPAIR_ID, new RepairPartRequest(PART_ID, 1, null));

        assertThat(guardado.getPriceCharged()).isEqualTo(PRECIO_VENTA);
    }

    @Test
    @DisplayName("REP-005: si no se envía cantidad se asume una unidad")
    void siNoSeEnviaCantidadSeAsumeUnaUnidad() {
        when(repairRepository.findById(REPAIR_ID)).thenReturn(Optional.of(reparacion));
        when(partCatalogService.findById(PART_ID)).thenReturn(Optional.of(repuesto));
        when(repairPartRepository.findByRepairId(REPAIR_ID)).thenReturn(List.of());
        devolverElRepuestoGuardado();

        RepairPart guardado = service.addPartToRepair(
                REPAIR_ID, new RepairPartRequest(PART_ID, null, PRECIO_VENTA));

        assertThat(guardado.getQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("REP-005: sin existencia suficiente se rechaza y no se descuenta inventario")
    void sinExistenciaSuficienteSeRechaza() {
        repuesto.setQuantity(1);
        when(repairRepository.findById(REPAIR_ID)).thenReturn(Optional.of(reparacion));
        when(partCatalogService.findById(PART_ID)).thenReturn(Optional.of(repuesto));

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> service.addPartToRepair(
                        REPAIR_ID, new RepairPartRequest(PART_ID, 5, PRECIO_VENTA)))
                .withMessageContaining("Stock insuficiente");

        verify(repairPartRepository, never()).save(any(RepairPart.class));
        verify(inventoryMovementService, never())
                .createRepairUseMovement(any(), any(), any(), any());
    }

    @Test
    @DisplayName("REP-005: una reparación inexistente produce 404")
    void unaReparacionInexistenteProduce404() {
        when(repairRepository.findById(REPAIR_ID)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> service.addPartToRepair(
                        REPAIR_ID, new RepairPartRequest(PART_ID, 1, PRECIO_VENTA)))
                .satisfies(excepcion ->
                        assertThat(excepcion.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ---------------------------------------------------------------- REP-002

    @Test
    @DisplayName("REP-002: el costo total es la mano de obra más los repuestos por su cantidad")
    void elCostoTotalSumaManoDeObraYRepuestos() {
        when(repairRepository.findById(REPAIR_ID)).thenReturn(Optional.of(reparacion));
        when(partCatalogService.findById(PART_ID)).thenReturn(Optional.of(repuesto));
        when(repairPartRepository.findByRepairId(REPAIR_ID))
                .thenReturn(List.of(repuestoDeReparacion(2, PRECIO_VENTA)));
        devolverElRepuestoGuardado();

        service.addPartToRepair(REPAIR_ID, new RepairPartRequest(PART_ID, 2, PRECIO_VENTA));

        ArgumentCaptor<Repair> capturada = ArgumentCaptor.forClass(Repair.class);
        verify(repairRepository).save(capturada.capture());

        Repair guardada = capturada.getValue();
        assertThat(guardada.getTotalPartsCost()).isEqualTo(2 * PRECIO_VENTA);
        assertThat(guardada.getTotalCost()).isEqualTo(MANO_DE_OBRA + 2 * PRECIO_VENTA);
    }

    @Test
    @DisplayName("REP-002: varios repuestos distintos se acumulan en el costo de la reparación")
    void variosRepuestosSeAcumulanEnElCosto() {
        when(repairRepository.findById(REPAIR_ID)).thenReturn(Optional.of(reparacion));
        when(partCatalogService.findById(PART_ID)).thenReturn(Optional.of(repuesto));
        when(repairPartRepository.findByRepairId(REPAIR_ID)).thenReturn(List.of(
                repuestoDeReparacion(2, PRECIO_VENTA),
                repuestoDeReparacion(3, 25000.0)));
        devolverElRepuestoGuardado();

        service.addPartToRepair(REPAIR_ID, new RepairPartRequest(PART_ID, 2, PRECIO_VENTA));

        ArgumentCaptor<Repair> capturada = ArgumentCaptor.forClass(Repair.class);
        verify(repairRepository).save(capturada.capture());

        double esperado = (2 * PRECIO_VENTA) + (3 * 25000.0);
        assertThat(capturada.getValue().getTotalPartsCost()).isEqualTo(esperado);
        assertThat(capturada.getValue().getTotalCost()).isEqualTo(MANO_DE_OBRA + esperado);
    }

    @Test
    @DisplayName("REP-002: un repuesto sin precio cobrado se cuenta como cero, no rompe el cálculo")
    void unRepuestoSinPrecioSeCuentaComoCero() {
        RepairPart sinPrecio = repuestoDeReparacion(2, PRECIO_VENTA);
        sinPrecio.setPriceCharged(null);

        when(repairRepository.findById(REPAIR_ID)).thenReturn(Optional.of(reparacion));
        when(partCatalogService.findById(PART_ID)).thenReturn(Optional.of(repuesto));
        when(repairPartRepository.findByRepairId(REPAIR_ID)).thenReturn(List.of(sinPrecio));
        devolverElRepuestoGuardado();

        service.addPartToRepair(REPAIR_ID, new RepairPartRequest(PART_ID, 1, PRECIO_VENTA));

        ArgumentCaptor<Repair> capturada = ArgumentCaptor.forClass(Repair.class);
        verify(repairRepository).save(capturada.capture());

        assertThat(capturada.getValue().getTotalPartsCost()).isZero();
        assertThat(capturada.getValue().getTotalCost()).isEqualTo(MANO_DE_OBRA);
    }

    // ---------------------------------------------------------------- REP-007

    @Test
    @DisplayName("REP-007: retirar un repuesto genera el movimiento de devolución al inventario")
    void retirarUnRepuestoGeneraElMovimientoDeDevolucion() {
        RepairPart parte = repuestoDeReparacion(3, PRECIO_VENTA);
        when(repairPartRepository.findById(500L)).thenReturn(Optional.of(parte));
        when(repairPartRepository.findByRepairId(REPAIR_ID)).thenReturn(List.of());

        service.removePartFromRepair(500L);

        verify(repairPartRepository).delete(parte);
        verify(inventoryMovementService).createRepairReturnMovement(
                eq(repuesto), eq(reparacion), eq(3), any(String.class));
    }

    @Test
    @DisplayName("REP-007: al retirar el último repuesto el costo vuelve a ser solo mano de obra")
    void alRetirarElUltimoRepuestoElCostoVuelveAManoDeObra() {
        reparacion.setTotalPartsCost(2 * PRECIO_VENTA);
        reparacion.recalculateTotalCost();

        RepairPart parte = repuestoDeReparacion(2, PRECIO_VENTA);
        when(repairPartRepository.findById(500L)).thenReturn(Optional.of(parte));
        when(repairPartRepository.findByRepairId(REPAIR_ID)).thenReturn(List.of());

        service.removePartFromRepair(500L);

        ArgumentCaptor<Repair> capturada = ArgumentCaptor.forClass(Repair.class);
        verify(repairRepository).save(capturada.capture());

        assertThat(capturada.getValue().getTotalPartsCost()).isZero();
        assertThat(capturada.getValue().getTotalCost()).isEqualTo(MANO_DE_OBRA);
    }

    @Test
    @DisplayName("REP-007: retirar un repuesto inexistente produce 404")
    void retirarUnRepuestoInexistenteProduce404() {
        when(repairPartRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> service.removePartFromRepair(999L))
                .satisfies(excepcion ->
                        assertThat(excepcion.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(inventoryMovementService, never())
                .createRepairReturnMovement(any(), any(), any(), any());
    }
}
