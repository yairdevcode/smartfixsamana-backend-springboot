package com.smartfixsamana.auth.controller;

import com.smartfixsamana.auth.JwtTokenProvider;
import com.smartfixsamana.auth.dto.LoginRequestDTO;
import com.smartfixsamana.auth.dto.LoginResponseDTO;
import com.smartfixsamana.models.entities.UserLogin;
import com.smartfixsamana.models.repositories.IUserLoginRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    private final JwtTokenProvider jwtTokenProvider;

    private final IUserLoginRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, IUserLoginRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {

        // Autenticar al usuario
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        // Generar token
        String token = jwtTokenProvider.generateToken(authentication);

        // Buscar el usuario desde la base de datos
        Optional<UserLogin> userOptional = userRepository.findByUsername(request.username());

        if (userOptional.isPresent()) {
            UserLogin user = userOptional.get();

            // Determinar si es admin según sus roles
            boolean admin = user.getRoles().stream()
                    .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));

            // Armar la respuesta
            LoginResponseDTO response = new LoginResponseDTO(
                    token,
                    user.getUsername(),
                    admin,
                    "Hola " + user.getUsername() + ", has iniciado sesión con éxito"
            );

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(404).body("Usuario no encontrado");
    }
}
