package com.smartfixsamana.models.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartfixsamana.models.entities.Customer;

import java.util.List;

public interface ICustomerRepository extends JpaRepository<Customer, Long> {

    @Query(value = "SELECT c FROM Customer c " +
            "WHERE LOWER(CONCAT(c.name, ' ', c.lastname)) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Customer> findCustomersByKeyword(@Param("keyword") String keyword, Pageable pageable);

}
