package com.smartfixsamana.models.services;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.smartfixsamana.models.entities.Role;
import com.smartfixsamana.models.entities.UserLogin;
import com.smartfixsamana.models.repositories.IUserLoginRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaUserDetailsServiceTest {

    private static final String USERNAME = "admin";
    private static final String EMAIL = "cangricelreparaciones@gmail.com";
    private static final String PASSWORD_HASH = "$2a$10$SdLGeLGbdX34zq0jwvZ.h.bN7WcVlaTkMyiBZUnSmhzUbeDdjGvFG";

    @Mock
    private IUserLoginRepository userRepository;

    private JpaUserDetailsService service;

    private UserLogin storedUser;

    @BeforeEach
    void setUp() {
        service = new JpaUserDetailsService(userRepository);

        storedUser = new UserLogin();
        storedUser.setId(1L);
        storedUser.setUsername(USERNAME);
        storedUser.setEmail(EMAIL);
        storedUser.setPassword(PASSWORD_HASH);
        storedUser.setRoles(List.of(new Role("ROLE_USER"), new Role("ROLE_ADMIN")));

        // Every lookup misses unless a test stubs the matching one.
        lenient().when(userRepository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.empty());
        lenient().when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void resolvesUserByUsername() {
        when(userRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(storedUser));

        UserDetails details = service.loadUserByUsername(USERNAME);

        assertThat(details.getUsername()).isEqualTo(USERNAME);
        assertThat(details.getPassword()).isEqualTo(PASSWORD_HASH);
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void resolvesUserByEmail() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(storedUser));

        UserDetails details = service.loadUserByUsername(EMAIL);

        assertThat(details.getPassword()).isEqualTo(PASSWORD_HASH);
    }

    @Test
    void returnsCanonicalUsernameAsPrincipalWhenResolvedByEmail() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(storedUser));

        UserDetails details = service.loadUserByUsername(EMAIL);

        // The principal becomes the JWT subject, so it must never be the email.
        assertThat(details.getUsername()).isEqualTo(USERNAME);
    }

    @Test
    void resolvesUserByEmailWithDifferentCasing() {
        String mixedCaseEmail = "CangricelReparaciones@Gmail.COM";
        when(userRepository.findByEmailIgnoreCase(mixedCaseEmail)).thenReturn(Optional.of(storedUser));

        UserDetails details = service.loadUserByUsername(mixedCaseEmail);

        assertThat(details.getUsername()).isEqualTo(USERNAME);
    }

    @Test
    void resolvesIdentifierWithSurroundingWhitespace() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(storedUser));
        when(userRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(storedUser));

        assertThat(service.loadUserByUsername("  " + EMAIL + " ").getUsername()).isEqualTo(USERNAME);
        assertThat(service.loadUserByUsername(" " + USERNAME + "  ").getUsername()).isEqualTo(USERNAME);
    }

    @Test
    void fallsBackToTheOtherColumnWhenTheFirstLookupMisses() {
        // An identifier without '@' that is actually stored as the email.
        String emailWithoutAtSign = "local-account";
        when(userRepository.findByEmailIgnoreCase(emailWithoutAtSign)).thenReturn(Optional.of(storedUser));

        assertThat(service.loadUserByUsername(emailWithoutAtSign).getUsername()).isEqualTo(USERNAME);
    }

    @Test
    void throwsUsernameNotFoundForUnknownIdentifier() {
        assertThatThrownBy(() -> service.loadUserByUsername("nobody@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                // The message must not reveal whether the account exists.
                .hasMessageNotContaining("nobody@example.com");
    }

    @Test
    void throwsUsernameNotFoundForBlankIdentifier() {
        assertThatThrownBy(() -> service.loadUserByUsername("   "))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
