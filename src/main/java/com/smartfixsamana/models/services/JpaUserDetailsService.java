package com.smartfixsamana.models.services;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.smartfixsamana.models.entities.UserLogin;
import com.smartfixsamana.models.repositories.IUserLoginRepository;

@Service
public class JpaUserDetailsService implements UserDetailsService {

    private final IUserLoginRepository iUserRepository;

    public JpaUserDetailsService(IUserLoginRepository iUserRepository) {
        this.iUserRepository = iUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserLogin> userOptional = iUserRepository.findByUsername(username);

        if (userOptional.isEmpty()) {

            throw new UsernameNotFoundException(String.format("Username %s no existe en el sistema", username));

        }
        UserLogin user = userOptional.orElseThrow();

        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(username, user.getPassword(), true, true, true, true, authorities);
    }
}
