package com.smartfixsamana.models.repositories;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.smartfixsamana.models.entities.UserLogin;


public interface IUserLoginRepository extends CrudRepository<UserLogin, Long>{

    Optional<UserLogin> findByUsernameIgnoreCase(String username);

    Optional<UserLogin> findByEmailIgnoreCase(String email);

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);


}
