package com.smartfixsamana.models.repositories;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.smartfixsamana.models.entities.PartCatalog;
import com.smartfixsamana.models.entities.Phone;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PartCatalogRepositorySearchTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private IPartCatalogRepository repository;

    private Phone samsungA10;
    private Phone xiaomiRedmiNote9;

    @BeforeEach
    void setUp() {
        samsungA10 = givenPhone("Samsung", "A10");
        xiaomiRedmiNote9 = givenPhone("Xiaomi", "Redmi Note 9");

        givenPart("Pantalla", samsungA10, 3);
        givenPart("Bateria", samsungA10, 5);
        givenPart("Pantalla", xiaomiRedmiNote9, 2);
        givenPart("Flex de carga", xiaomiRedmiNote9, 0); // out of stock
        givenPart("Tornillos surtidos", null, 10);       // no phone assigned

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findsByPartName() {
        assertThat(namesOf(repository.searchAvailableParts("bateria", null)))
                .containsExactly("Bateria");
    }

    @Test
    void findsByPhoneBrand() {
        assertThat(repository.searchAvailableParts("samsung", null))
                .extracting(part -> part.getName() + " " + part.getPhone().getModel())
                .containsExactlyInAnyOrder("Bateria A10", "Pantalla A10");
    }

    @Test
    void findsByPhoneModel() {
        assertThat(repository.searchAvailableParts("Redmi Note 9", null))
                .extracting(PartCatalog::getName)
                .containsExactly("Pantalla");
    }

    @Test
    void isCaseInsensitive() {
        List<PartCatalog> lowercase = repository.searchAvailableParts("samsung", null);
        List<PartCatalog> uppercase = repository.searchAvailableParts("SAMSUNG", null);

        assertThat(namesOf(lowercase)).isEqualTo(namesOf(uppercase));
        assertThat(lowercase).hasSize(2);
    }

    @Test
    void matchesMultiWordKeywordSpanningNameAndPhone() {
        // "pantalla samsung" only matches once name, brand and model are concatenated.
        assertThat(repository.searchAvailableParts("pantalla samsung", null))
                .singleElement()
                .satisfies(part -> {
                    assertThat(part.getName()).isEqualTo("Pantalla");
                    assertThat(part.getPhone().getBrand()).isEqualTo("Samsung");
                });
    }

    @Test
    void excludesPartsWithoutStock() {
        assertThat(repository.searchAvailableParts("flex", null)).isEmpty();
    }

    @Test
    void findsPartWithNullPhoneByItsName() {
        // COALESCE guard: without it CONCAT would be NULL and this part would vanish.
        assertThat(repository.searchAvailableParts("tornillos", null))
                .singleElement()
                .satisfies(part -> {
                    assertThat(part.getName()).isEqualTo("Tornillos surtidos");
                    assertThat(part.getPhone()).isNull();
                });
    }

    @Test
    void restrictsResultsToTheGivenPhoneId() {
        assertThat(repository.searchAvailableParts("pantalla", samsungA10.getId()))
                .singleElement()
                .satisfies(part -> assertThat(part.getPhone().getId()).isEqualTo(samsungA10.getId()));

        assertThat(repository.searchAvailableParts(null, xiaomiRedmiNote9.getId()))
                .extracting(PartCatalog::getName)
                .containsExactly("Pantalla"); // the out-of-stock flex is still excluded
    }

    private Phone givenPhone(String brand, String model) {
        Phone phone = new Phone();
        phone.setBrand(brand);
        phone.setModel(model);
        return entityManager.persist(phone);
    }

    private PartCatalog givenPart(String name, Phone phone, int quantity) {
        PartCatalog part = new PartCatalog();
        part.setName(name);
        part.setPhone(phone);
        part.setQuantity(quantity);
        return entityManager.persist(part);
    }

    private static List<String> namesOf(List<PartCatalog> parts) {
        return parts.stream().map(PartCatalog::getName).toList();
    }
}
