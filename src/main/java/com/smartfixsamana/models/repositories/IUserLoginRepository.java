package com.smartfixsamana.models.repositories;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.smartfixsamana.models.entities.UserLogin;


public interface IUserLoginRepository extends CrudRepository<UserLogin, Long>{

    Optional<UserLogin> findByUsername(String username);

}
