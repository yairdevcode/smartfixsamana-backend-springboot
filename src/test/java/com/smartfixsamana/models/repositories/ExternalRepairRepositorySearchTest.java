package com.smartfixsamana.models.repositories;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.smartfixsamana.models.entities.ExternalRepair;
import com.smartfixsamana.models.enums.ExternalRepairStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ExternalRepairRepositorySearchTest {

    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20, Sort.by("date").descending());

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private IExternalRepairRepository repository;

    @BeforeEach
    void setUp() {
        givenRepair("Rodrigo Perez", "Moto G04", ExternalRepairStatus.ENTREGADO, LocalDate.of(2026, 1, 10));
        givenRepair("Ana Martinez", "Honor Magic 7 Lite", ExternalRepairStatus.REPARADO, LocalDate.of(2026, 2, 15));
        givenRepair("Luis Gomez", "Moto G54", ExternalRepairStatus.ENTREGADO, LocalDate.of(2026, 3, 20));
        givenRepair("Carmen Diaz", "Samsung A15", ExternalRepairStatus.PENDIENTE_RECOGER, LocalDate.of(2026, 4, 5));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findsByPartialClientName() {
        assertThat(clientsOf(repository.findWithFilters(null, null, null, "rodri", FIRST_PAGE)))
                .containsExactly("Rodrigo Perez");
    }

    @Test
    void findsByPartialPhoneBrandOrModel() {
        assertThat(clientsOf(repository.findWithFilters(null, null, null, "honor magic", FIRST_PAGE)))
                .containsExactly("Ana Martinez");

        assertThat(clientsOf(repository.findWithFilters(null, null, null, "moto", FIRST_PAGE)))
                .containsExactlyInAnyOrder("Rodrigo Perez", "Luis Gomez");
    }

    @Test
    void isCaseInsensitive() {
        Page<ExternalRepair> lowercase = repository.findWithFilters(null, null, null, "moto", FIRST_PAGE);
        Page<ExternalRepair> uppercase = repository.findWithFilters(null, null, null, "MOTO", FIRST_PAGE);

        assertThat(clientsOf(uppercase)).isEqualTo(clientsOf(lowercase));
        assertThat(uppercase).hasSize(2);
    }

    @Test
    void matchesMultiWordKeywordSpanningClientAndPhone() {
        // "perez moto" only matches once clientName and phoneBrand are concatenated: LIKE
        // matches a contiguous substring of "Rodrigo Perez Moto G04", so the words have to
        // meet at the boundary the way they do in searchAvailableParts.
        assertThat(repository.findWithFilters(null, null, null, "perez moto", FIRST_PAGE))
                .singleElement()
                .satisfies(repair -> {
                    assertThat(repair.getClientName()).isEqualTo("Rodrigo Perez");
                    assertThat(repair.getPhoneBrand()).isEqualTo("Moto G04");
                });
    }

    @Test
    void nullKeywordReturnsEveryRow() {
        assertThat(repository.findWithFilters(null, null, null, null, FIRST_PAGE))
                .hasSize(4);
    }

    @Test
    void narrowsWhenCombinedWithStatus() {
        assertThat(clientsOf(repository.findWithFilters(
                ExternalRepairStatus.ENTREGADO, null, null, "moto", FIRST_PAGE)))
                .containsExactlyInAnyOrder("Rodrigo Perez", "Luis Gomez");

        assertThat(repository.findWithFilters(
                ExternalRepairStatus.REPARADO, null, null, "moto", FIRST_PAGE))
                .isEmpty();
    }

    @Test
    void narrowsWhenCombinedWithDateRange() {
        assertThat(clientsOf(repository.findWithFilters(
                null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), "moto", FIRST_PAGE)))
                .containsExactly("Luis Gomez");

        assertThat(repository.findWithFilters(
                null, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), "moto", FIRST_PAGE))
                .isEmpty();
    }

    private ExternalRepair givenRepair(String clientName, String phoneBrand,
                                       ExternalRepairStatus status, LocalDate date) {
        ExternalRepair repair = new ExternalRepair();
        repair.setClientName(clientName);
        repair.setPhoneBrand(phoneBrand);
        repair.setSolution("Cambio de pantalla");
        repair.setRepairPrice(3500.0);
        repair.setPartCost(1200.0);
        repair.setStatus(status);
        repair.setDate(date);
        return entityManager.persist(repair);
    }

    private static List<String> clientsOf(Page<ExternalRepair> repairs) {
        return repairs.getContent().stream().map(ExternalRepair::getClientName).toList();
    }
}
