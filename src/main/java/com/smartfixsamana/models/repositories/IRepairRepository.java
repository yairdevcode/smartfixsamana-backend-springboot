package com.smartfixsamana.models.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.smartfixsamana.models.entities.Repair;

public interface IRepairRepository extends CrudRepository<Repair, Long> {

@Query("SELECT r FROM Repair r " +
        "WHERE LOWER(CONCAT(r.customer.name, ' ', r.customer.lastname," +
        " ' ', r.phone.brand, ' ', r.phone.model)) LIKE LOWER(CONCAT('%', :keyword, '%'))")
Page<Repair> findRepairsByKeyword(@Param("keyword") String keyword, Pageable pageable);


}
