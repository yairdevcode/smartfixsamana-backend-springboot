package com.smartfixsamana.models.services;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.smartfixsamana.models.dto.InventoryMovementDTO;
import com.smartfixsamana.models.entities.InventoryMovement;
import com.smartfixsamana.models.entities.PartCatalog;
import com.smartfixsamana.models.enums.MovementType;
import com.smartfixsamana.models.repositories.IInventoryMovementRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Casos INV-001, INV-002 e INV-003 del plan de pruebas GA9-220501096-AA1-EV02.
 *
 * Verifica que cada movimiento de inventario ajuste las existencias del catálogo
 * en la dirección y magnitud correctas, y que una salida mayor que la existencia
 * disponible sea rechazada sin alterar el catálogo.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryMovementService - ajuste de existencias")
class InventoryMovementServiceTest {

    private static final Long PART_ID = 1L;
    private static final int EXISTENCIA_INICIAL = 10;

    @Mock
    private IInventoryMovementRepository inventoryMovementRepository;

    @Mock
    private PartCatalogService partCatalogService;

    private InventoryMovementService service;
    private PartCatalog repuesto;

    @BeforeEach
    void setUp() {
        service = new InventoryMovementService(inventoryMovementRepository, partCatalogService);

        repuesto = new PartCatalog();
        repuesto.setId(PART_ID);
        repuesto.setName("Pantalla Samsung A15");
        repuesto.setQuantity(EXISTENCIA_INICIAL);
        repuesto.setMinStock(5);
        repuesto.setPurchasePrice(85000.0);
        repuesto.setSalePrice(140000.0);
    }

    /** Devuelve el mismo movimiento que se le pasa, como haría el repositorio real. */
    private void devolverElMovimientoGuardado() {
        when(inventoryMovementRepository.save(any(InventoryMovement.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private InventoryMovementDTO movimiento(MovementType tipo, int cantidad) {
        return new InventoryMovementDTO(PART_ID, tipo, cantidad, "Prueba automatizada", null);
    }

    // ---------------------------------------------------------------- INV-001

    @Test
    @DisplayName("INV-001: una compra incrementa las existencias en la cantidad registrada")
    void unaCompraIncrementaLasExistencias() {
        when(partCatalogService.findById(PART_ID)).thenReturn(Optional.of(repuesto));
        devolverElMovimientoGuardado();

        InventoryMovement resultado = service.save(movimiento(MovementType.PURCHASE, 5));

        assertThat(repuesto.getQuantity()).isEqualTo(EXISTENCIA_INICIAL + 5);
        assertThat(resultado.getMovementType()).isEqualTo(MovementType.PURCHASE);
        assertThat(resultado.getQuantity()).isEqualTo(5);
        verify(partCatalogService).save(repuesto);
    }

    @Test
    @DisplayName("INV-001: la devolución desde una reparación también incrementa existencias")
    void laDevolucionDesdeUnaReparacionIncrementaLasExistencias() {
        when(partCatalogService.findById(PART_ID)).thenReturn(Optional.of(repuesto));
        devolverElMovimientoGuardado();

        service.save(movimiento(MovementType.REPAIR_RETURN, 3));

        assertThat(repuesto.getQuantity()).isEqualTo(EXISTENCIA_INICIAL + 3);
    }

    // ---------------------------------------------------------------- INV-002

    @Test
    @DisplayName("INV-002: una venta disminuye las existencias en la cantidad registrada")
    void unaVentaDisminuyeLasExistencias() {
        when(partCatalogService.findById(PART_ID)).thenReturn(Optional.of(repuesto));
        devolverElMovimientoGuardado();

        InventoryMovement resultado = service.save(movimiento(MovementType.SALE, 4));

        assertThat(repuesto.getQuantity()).isEqualTo(EXISTENCIA_INICIAL - 4);
        assertThat(resultado.getMovementType()).isEqualTo(MovementType.SALE);
        verify(partCatalogService).save(repuesto);
    }

    @Test
    @DisplayName("INV-002: el consumo en una reparación también disminuye existencias")
    void elConsumoEnUnaReparacionDisminuyeLasExistencias() {
        when(partCatalogService.findById(PART_ID)).thenReturn(Optional.of(repuesto));
        devolverElMovimientoGuardado();

        service.save(movimiento(MovementType.REPAIR_USE, 2));

        assertThat(repuesto.getQuantity()).isEqualTo(EXISTENCIA_INICIAL - 2);
    }

    @Test
    @DisplayName("INV-002: una salida por la existencia completa deja el repuesto en cero")
    void unaSalidaPorLaExistenciaCompletaDejaElRepuestoEnCero() {
        when(partCatalogService.findById(PART_ID)).thenReturn(Optional.of(repuesto));
        devolverElMovimientoGuardado();

        service.save(movimiento(MovementType.SALE, EXISTENCIA_INICIAL));

        assertThat(repuesto.getQuantity()).isZero();
    }

    // ---------------------------------------------------------------- INV-003

    @Test
    @DisplayName("INV-003: una salida mayor que la existencia disponible es rechazada")
    void unaSalidaMayorQueLaExistenciaEsRechazada() {
        when(partCatalogService.findById(PART_ID)).thenReturn(Optional.of(repuesto));

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> service.save(movimiento(MovementType.SALE, 20)))
                .withMessageContaining("Stock insuficiente")
                .satisfies(excepcion ->
                        assertThat(excepcion.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("INV-003: tras el rechazo la existencia permanece intacta y no se guarda nada")
    void trasElRechazoLaExistenciaPermaneceIntacta() {
        when(partCatalogService.findById(PART_ID)).thenReturn(Optional.of(repuesto));

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> service.save(movimiento(MovementType.REPAIR_USE, 11)));

        assertThat(repuesto.getQuantity()).isEqualTo(EXISTENCIA_INICIAL);
        verify(partCatalogService, never()).save(any(PartCatalog.class));
        verify(inventoryMovementRepository, never()).save(any(InventoryMovement.class));
    }

    @Test
    @DisplayName("INV-003: un repuesto inexistente produce 404 y no altera el inventario")
    void unRepuestoInexistenteProduce404() {
        when(partCatalogService.findById(PART_ID)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> service.save(movimiento(MovementType.PURCHASE, 5)))
                .satisfies(excepcion ->
                        assertThat(excepcion.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(inventoryMovementRepository, never()).save(any(InventoryMovement.class));
    }

    // ------------------------------------------------- consistencia acumulada

    @Test
    @DisplayName("INV-004: una secuencia de entradas y salidas deja la existencia correcta")
    void unaSecuenciaDeMovimientosDejaLaExistenciaCorrecta() {
        when(partCatalogService.findById(PART_ID)).thenReturn(Optional.of(repuesto));
        devolverElMovimientoGuardado();

        service.save(movimiento(MovementType.PURCHASE, 20));   // 10 + 20 = 30
        service.save(movimiento(MovementType.REPAIR_USE, 6));  // 30 -  6 = 24
        service.save(movimiento(MovementType.SALE, 4));        // 24 -  4 = 20
        service.save(movimiento(MovementType.REPAIR_RETURN, 2)); // 20 + 2 = 22
        service.save(movimiento(MovementType.DAMAGE, 2));      // 22 -  2 = 20

        assertThat(repuesto.getQuantity()).isEqualTo(20);
        verify(inventoryMovementRepository, times(5)).save(any(InventoryMovement.class));
    }
}
