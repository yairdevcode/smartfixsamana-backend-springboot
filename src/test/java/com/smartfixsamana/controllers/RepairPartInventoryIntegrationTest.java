package com.smartfixsamana.controllers;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfixsamana.models.dto.InventoryMovementDTO;
import com.smartfixsamana.models.dto.RepairPartRequest;
import com.smartfixsamana.models.entities.InventoryMovement;
import com.smartfixsamana.models.entities.PartCatalog;
import com.smartfixsamana.models.entities.Repair;
import com.smartfixsamana.models.entities.RepairPart;
import com.smartfixsamana.models.enums.MovementType;
import com.smartfixsamana.models.repositories.IInventoryMovementRepository;
import com.smartfixsamana.models.repositories.IPartCatalogRepository;
import com.smartfixsamana.models.repositories.IRepairPartRepository;
import com.smartfixsamana.models.repositories.IRepairRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Casos REP-005, REP-007 e INV-004 del plan de pruebas GA9-220501096-AA1-EV02.
 *
 * A diferencia de las pruebas unitarias de RepairPartServiceTest e
 * InventoryMovementServiceTest, que sustituyen los repositorios por
 * simulacros, aquí la petición atraviesa el sistema completo: enrutamiento,
 * serialización JSON, reglas de seguridad, servicio, JPA y base de datos.
 * El objetivo es comprobar que las existencias persistidas coinciden con los
 * movimientos registrados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Integración - consistencia entre reparaciones e inventario")
class RepairPartInventoryIntegrationTest {

    private static final double PRECIO_VENTA = 140000.0;
    private static final double MANO_DE_OBRA = 30000.0;
    private static final int EXISTENCIA_INICIAL = 10;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IPartCatalogRepository partCatalogRepository;

    @Autowired
    private IRepairRepository repairRepository;

    @Autowired
    private IRepairPartRepository repairPartRepository;

    @Autowired
    private IInventoryMovementRepository inventoryMovementRepository;

    private PartCatalog repuesto;
    private Repair reparacion;

    @BeforeEach
    void prepararDatos() {
        repairPartRepository.deleteAll();
        inventoryMovementRepository.deleteAll();
        repairRepository.deleteAll();
        partCatalogRepository.deleteAll();

        repuesto = new PartCatalog();
        repuesto.setName("Pantalla Samsung A15");
        repuesto.setDescription("Repuesto para pruebas de integración");
        repuesto.setQuantity(EXISTENCIA_INICIAL);
        repuesto.setMinStock(5);
        repuesto.setPurchasePrice(85000.0);
        repuesto.setSalePrice(PRECIO_VENTA);
        repuesto = partCatalogRepository.save(repuesto);

        reparacion = new Repair();
        reparacion.setFault("No enciende");
        reparacion.setState("EN_PROCESO");
        reparacion.setDate(LocalDate.now());
        reparacion.setLaborCost(MANO_DE_OBRA);
        reparacion = repairRepository.save(reparacion);
    }

    private String json(Object cuerpo) throws Exception {
        return objectMapper.writeValueAsString(cuerpo);
    }

    private int existenciaPersistida() {
        return partCatalogRepository.findById(repuesto.getId()).orElseThrow().getQuantity();
    }

    private void agregarRepuesto(int cantidad) throws Exception {
        mockMvc.perform(post("/api/repairs/{id}/parts", reparacion.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RepairPartRequest(repuesto.getId(), cantidad, PRECIO_VENTA))))
                .andExpect(status().isCreated());
    }

    private void registrarMovimiento(MovementType tipo, int cantidad) throws Exception {
        mockMvc.perform(post("/api/inventory-movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new InventoryMovementDTO(
                                repuesto.getId(), tipo, cantidad, "Prueba de integración", null))))
                .andExpect(status().is2xxSuccessful());
    }

    // ---------------------------------------------------------------- REP-005

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("REP-005: agregar un repuesto a una reparación descuenta la existencia persistida")
    void agregarRepuestoDescuentaLaExistenciaPersistida() throws Exception {
        agregarRepuesto(3);

        assertThat(existenciaPersistida()).isEqualTo(EXISTENCIA_INICIAL - 3);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("REP-005: el descuento queda registrado como movimiento de inventario")
    void elDescuentoQuedaRegistradoComoMovimiento() throws Exception {
        agregarRepuesto(3);

        List<InventoryMovement> movimientos = inventoryMovementRepository.findAll();
        assertThat(movimientos).hasSize(1);
        assertThat(movimientos.get(0).getMovementType()).isEqualTo(MovementType.REPAIR_USE);
        assertThat(movimientos.get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("REP-005: el costo de la reparación se actualiza en la base de datos")
    void elCostoDeLaReparacionSeActualizaEnBaseDeDatos() throws Exception {
        agregarRepuesto(2);

        Repair persistida = repairRepository.findById(reparacion.getId()).orElseThrow();
        assertThat(persistida.getTotalPartsCost()).isEqualTo(2 * PRECIO_VENTA);
        assertThat(persistida.getTotalCost()).isEqualTo(MANO_DE_OBRA + 2 * PRECIO_VENTA);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("REP-005: sin existencia suficiente responde 400 y no altera el inventario")
    void sinExistenciaSuficienteRespondeCuatrocientos() throws Exception {
        mockMvc.perform(post("/api/repairs/{id}/parts", reparacion.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RepairPartRequest(repuesto.getId(), 50, PRECIO_VENTA))))
                .andExpect(status().isBadRequest());

        assertThat(existenciaPersistida()).isEqualTo(EXISTENCIA_INICIAL);
        assertThat(inventoryMovementRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("REP-005: sin autenticación el endpoint queda bloqueado")
    void sinAutenticacionElEndpointQuedaBloqueado() throws Exception {
        mockMvc.perform(post("/api/repairs/{id}/parts", reparacion.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RepairPartRequest(repuesto.getId(), 1, PRECIO_VENTA))))
                .andExpect(status().is4xxClientError());

        assertThat(existenciaPersistida()).isEqualTo(EXISTENCIA_INICIAL);
    }

    // ---------------------------------------------------------------- REP-007

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("REP-007: retirar el repuesto devuelve la existencia al catálogo")
    void retirarElRepuestoDevuelveLaExistencia() throws Exception {
        agregarRepuesto(4);
        assertThat(existenciaPersistida()).isEqualTo(EXISTENCIA_INICIAL - 4);

        RepairPart agregado = repairPartRepository.findByRepairId(reparacion.getId()).get(0);

        mockMvc.perform(delete("/api/repairs/{repairId}/parts/{repairPartId}",
                        reparacion.getId(), agregado.getId()))
                .andExpect(status().isNoContent());

        assertThat(existenciaPersistida()).isEqualTo(EXISTENCIA_INICIAL);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("REP-007: tras el retiro el costo vuelve a ser solo la mano de obra")
    void trasElRetiroElCostoVuelveAManoDeObra() throws Exception {
        agregarRepuesto(2);
        RepairPart agregado = repairPartRepository.findByRepairId(reparacion.getId()).get(0);

        mockMvc.perform(delete("/api/repairs/{repairId}/parts/{repairPartId}",
                        reparacion.getId(), agregado.getId()))
                .andExpect(status().isNoContent());

        Repair persistida = repairRepository.findById(reparacion.getId()).orElseThrow();
        assertThat(persistida.getTotalPartsCost()).isZero();
        assertThat(persistida.getTotalCost()).isEqualTo(MANO_DE_OBRA);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("REP-007: el retiro deja registrados los dos movimientos, consumo y devolución")
    void elRetiroDejaLosDosMovimientos() throws Exception {
        agregarRepuesto(2);
        RepairPart agregado = repairPartRepository.findByRepairId(reparacion.getId()).get(0);

        mockMvc.perform(delete("/api/repairs/{repairId}/parts/{repairPartId}",
                        reparacion.getId(), agregado.getId()))
                .andExpect(status().isNoContent());

        assertThat(inventoryMovementRepository.findAll())
                .extracting(InventoryMovement::getMovementType)
                .containsExactlyInAnyOrder(MovementType.REPAIR_USE, MovementType.REPAIR_RETURN);
    }

    // ---------------------------------------------------------------- INV-004

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("INV-004: una secuencia de movimientos deja la existencia igual a su suma algebraica")
    void unaSecuenciaDeMovimientosDejaLaExistenciaCorrecta() throws Exception {
        registrarMovimiento(MovementType.PURCHASE, 20);   // 10 + 20 = 30
        registrarMovimiento(MovementType.SALE, 4);        // 30 -  4 = 26
        registrarMovimiento(MovementType.DAMAGE, 2);      // 26 -  2 = 24

        assertThat(existenciaPersistida()).isEqualTo(24);
        assertThat(inventoryMovementRepository.findAll()).hasSize(3);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("INV-004: los movimientos de inventario y los de reparación se acumulan sobre el mismo repuesto")
    void losMovimientosDeAmbosOrigenesSeAcumulan() throws Exception {
        registrarMovimiento(MovementType.PURCHASE, 10);   // 10 + 10 = 20
        agregarRepuesto(6);                               // 20 -  6 = 14
        registrarMovimiento(MovementType.SALE, 4);        // 14 -  4 = 10

        assertThat(existenciaPersistida()).isEqualTo(10);
        assertThat(inventoryMovementRepository.findAll()).hasSize(3);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("INV-004: una salida mayor que la existencia responde 400 y no rompe la consistencia")
    void unaSalidaMayorQueLaExistenciaRespondeCuatrocientos() throws Exception {
        mockMvc.perform(post("/api/inventory-movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new InventoryMovementDTO(
                                repuesto.getId(), MovementType.SALE, 99, "Prueba", null))))
                .andExpect(status().isBadRequest());

        assertThat(existenciaPersistida()).isEqualTo(EXISTENCIA_INICIAL);
        assertThat(inventoryMovementRepository.findAll()).isEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("INV-004: la cantidad debe ser positiva, el cero se rechaza por validación")
    void laCantidadDebeSerPositiva() throws Exception {
        mockMvc.perform(post("/api/inventory-movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new InventoryMovementDTO(
                                repuesto.getId(), MovementType.PURCHASE, 0, "Prueba", null))))
                .andExpect(status().isBadRequest());

        assertThat(existenciaPersistida()).isEqualTo(EXISTENCIA_INICIAL);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("REP-005: la respuesta expone el repuesto agregado con su cantidad")
    void laRespuestaExponeElRepuestoAgregado() throws Exception {
        mockMvc.perform(post("/api/repairs/{id}/parts", reparacion.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RepairPartRequest(repuesto.getId(), 2, PRECIO_VENTA))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(2));
    }
}
