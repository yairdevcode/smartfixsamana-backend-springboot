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

    @Query("SELECT pc FROM PartCatalog pc WHERE " +
           "(:name IS NULL OR LOWER(pc.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:phoneId IS NULL OR pc.phone.id = :phoneId) AND " +
           "pc.quantity > 0")
    List<PartCatalog> searchAvailableParts(@Param("name") String name, @Param("phoneId") Long phoneId);

    /**
     * Paginated search with optional filters for name and phoneId.
     */
    @Query("SELECT pc FROM PartCatalog pc WHERE " +
           "(:name IS NULL OR LOWER(pc.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:phoneId IS NULL OR pc.phone.id = :phoneId)")
    Page<PartCatalog> searchPartsPaginated(
            @Param("name") String name,
            @Param("phoneId") Long phoneId,
            Pageable pageable);

    /**
     * Find all parts with pagination
     */
    Page<PartCatalog> findAll(Pageable pageable);

}
