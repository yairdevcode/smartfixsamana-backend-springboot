package com.smartfixsamana.models.repositories;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.smartfixsamana.models.entities.Phone;

public interface IPhoneRepository extends CrudRepository<Phone, Long> {

    @Query("SELECT p FROM Phone p " +
            "WHERE LOWER(CONCAT(p.brand, ' ', p.model)) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Phone> findPhonesByKeyword(@Param("keyword") String keyword, Pageable pageable);


}
