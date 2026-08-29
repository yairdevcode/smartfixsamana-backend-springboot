package com.smartfixsamana.models.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartfixsamana.models.entities.PartCatalog;

public interface IPartCatalogRepository extends JpaRepository<PartCatalog, Long> {

    @Query("SELECT pc FROM PartCatalog pc WHERE pc.quantity <= pc.minStock")
    List<PartCatalog> findLowStock();

    List<PartCatalog> findByNameContainingIgnoreCase(String name);

    List<PartCatalog> findByPhoneId(Long phoneId);

    List<PartCatalog> findByNameContainingIgnoreCaseAndPhoneId(String name, Long phoneId);

    List<PartCatalog> findByQuantityLessThanEqual(Integer quantity);

    List<PartCatalog> findByQuantityGreaterThan(Integer quantity);

    @Query("SELECT pc FROM PartCatalog pc WHERE " +
           "(:name IS NULL OR LOWER(pc.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:phoneId IS NULL OR pc.phone.id = :phoneId)")
    List<PartCatalog> searchParts(@Param("name") String name, @Param("phoneId") Long phoneId);

    /**
     * Search parts that are in stock (quantity &gt; 0).
     *
     * <p>The keyword is matched against the part name, the phone brand and the phone model,
     * joined into a single "name brand model" haystack, so a multi-word keyword such as
     * "pantalla samsung a10" matches too. Every field is wrapped in COALESCE because MySQL's
     * CONCAT returns NULL as soon as one argument is NULL, which would hide parts with no
     * phone assigned. The join is a LEFT JOIN for that same reason.
     */
    @Query("SELECT pc FROM PartCatalog pc LEFT JOIN pc.phone p WHERE " +
           "pc.quantity > 0 AND " +
           "(:phoneId IS NULL OR p.id = :phoneId) AND " +
           "(:name IS NULL OR LOWER(CONCAT(COALESCE(pc.name, ''), ' ', " +
           "COALESCE(p.brand, ''), ' ', COALESCE(p.model, ''))) " +
           "LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "ORDER BY pc.name ASC, p.brand ASC, p.model ASC")
    List<PartCatalog> searchAvailableParts(@Param("name") String name, @Param("phoneId") Long phoneId);

    /**
     * Paginated search with optional filters for name and phoneId.
     */
    @Query("SELECT pc FROM PartCatalog pc LEFT JOIN pc.phone p WHERE " +
           "(:name IS NULL OR LOWER(pc.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "OR LOWER(p.model) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:phoneId IS NULL OR p.id = :phoneId)")
    Page<PartCatalog> searchPartsPaginated(
            @Param("name") String name,
            @Param("phoneId") Long phoneId,
            Pageable pageable);

    /**
     * Find all parts with pagination
     */
    Page<PartCatalog> findAll(Pageable pageable);

}
