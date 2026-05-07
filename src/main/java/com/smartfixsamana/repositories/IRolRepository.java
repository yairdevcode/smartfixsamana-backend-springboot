package com.smartfixsamana.repositories;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.smartfixsamana.models.entities.Role;


public interface IRolRepository extends CrudRepository<Role, Long>{

    Optional<Role>  findByName(String name);

}
