package com.smartfixsamana.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartfixsamana.models.entities.PartType;

public interface IPartTypeRepository extends JpaRepository<PartType, Long> {

    Optional<PartType> findByName(String name);

    List<PartType> findByNameContainingIgnoreCase(String name);

    boolean existsByName(String name);

    List<PartType> findAllByOrderByNameAsc();

}
