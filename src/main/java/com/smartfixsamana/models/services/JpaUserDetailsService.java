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

    /**
     * Generic on purpose: it must never reveal whether the account exists.
     * It is only used internally, DaoAuthenticationProvider hides it behind
     * a BadCredentialsException before it reaches the client.
     */
    private static final String NOT_FOUND_MESSAGE = "Invalid credentials";

    private final IUserLoginRepository iUserRepository;

    public JpaUserDetailsService(IUserLoginRepository iUserRepository) {
        this.iUserRepository = iUserRepository;
    }

    /**
     * The identifier may be either the username or the email address, which is what
     * the login form offers ("Nombre de usuario o correo electronico").
     */
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        if (identifier == null || identifier.isBlank()) {
            throw new UsernameNotFoundException(NOT_FOUND_MESSAGE);
        }

        // Mobile keyboards often append a trailing space to the identifier.
        UserLogin user = resolveUser(identifier.trim())
                .orElseThrow(() -> new UsernameNotFoundException(NOT_FOUND_MESSAGE));

        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList());

        // The principal must be the canonical username, never the raw identifier:
        // it becomes the JWT subject and every downstream lookup relies on it.
        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), true,
                true, true, true, authorities);
    }

    /**
     * Resolves the account with a deterministic column order: an identifier holding
     * '@' is almost certainly an email, anything else is almost certainly a username.
     * The other column is still tried as a fallback so neither form can ever fail.
     */
    private Optional<UserLogin> resolveUser(String identifier) {
        if (identifier.contains("@")) {
            return iUserRepository.findByEmailIgnoreCase(identifier)
                    .or(() -> iUserRepository.findByUsernameIgnoreCase(identifier));
        }
        return iUserRepository.findByUsernameIgnoreCase(identifier)
                .or(() -> iUserRepository.findByEmailIgnoreCase(identifier));
    }
}
