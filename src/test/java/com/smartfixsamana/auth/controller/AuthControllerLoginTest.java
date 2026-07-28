package com.smartfixsamana.auth.controller;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfixsamana.auth.JwtTokenProvider;
import com.smartfixsamana.auth.dto.LoginResponseDTO;
import com.smartfixsamana.models.entities.Role;
import com.smartfixsamana.models.entities.UserLogin;
import com.smartfixsamana.models.repositories.IRolRepository;
import com.smartfixsamana.models.repositories.IUserLoginRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerLoginTest {

    private static final String USERNAME = "admin";
    private static final String EMAIL = "cangricelreparaciones@gmail.com";
    private static final String PASSWORD = "secret123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IUserLoginRepository userRepository;

    @Autowired
    private IRolRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void seedUser() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role userRole = roleRepository.save(new Role("ROLE_USER"));
        Role adminRole = roleRepository.save(new Role("ROLE_ADMIN"));

        UserLogin user = new UserLogin();
        user.setUsername(USERNAME);
        user.setEmail(EMAIL);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRoles(List.of(userRole, adminRole));
        userRepository.save(user);
    }

    @Test
    void loginWithUsernameReturnsCanonicalUsername() throws Exception {
        LoginResponseDTO response = login(USERNAME, PASSWORD);

        assertThat(response.username()).isEqualTo(USERNAME);
        assertThat(response.admin()).isTrue();
    }

    @Test
    void loginWithEmailReturnsCanonicalUsernameNotTheEmail() throws Exception {
        LoginResponseDTO response = login(EMAIL, PASSWORD);

        assertThat(response.username()).isEqualTo(USERNAME);
        assertThat(response.username()).isNotEqualTo(EMAIL);
        assertThat(response.admin()).isTrue();
    }

    @Test
    void loginWithEmailIssuesTokenWhoseSubjectIsTheCanonicalUsername() throws Exception {
        LoginResponseDTO response = login(EMAIL, PASSWORD);

        assertThat(jwtTokenProvider.validateToken(response.token())).isTrue();
        assertThat(jwtTokenProvider.getAuthentication(response.token()).getName()).isEqualTo(USERNAME);
    }

    @Test
    void loginWithPaddedMixedCaseEmailStillSucceeds() throws Exception {
        LoginResponseDTO response = login("  CangricelReparaciones@Gmail.COM ", PASSWORD);

        assertThat(response.username()).isEqualTo(USERNAME);
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(loginRequest(EMAIL, "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    void loginWithUnknownIdentifierReturnsTheSameErrorAsAWrongPassword() throws Exception {
        mockMvc.perform(loginRequest("nobody@example.com", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    private LoginResponseDTO login(String identifier, String password) throws Exception {
        String body = mockMvc.perform(loginRequest(identifier, password))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(body, LoginResponseDTO.class);
    }

    /**
     * The payload uses the wire field name "username" on purpose: that is what the
     * deployed frontend sends, whether the value is a username or an email.
     */
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginRequest(String identifier,
            String password) throws Exception {
        String payload = objectMapper.writeValueAsString(
                java.util.Map.of("username", identifier, "password", password));

        return post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload);
    }
}
