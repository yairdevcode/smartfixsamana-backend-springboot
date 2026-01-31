package com.smartfixsamana.models.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.smartfixsamana.models.entities.Role;
import com.smartfixsamana.models.entities.UserLogin;
import com.smartfixsamana.models.repositories.IRolRepository;
import com.smartfixsamana.models.repositories.IUserLoginRepository;


@Service
public class UserLoginService {

    private final IUserLoginRepository iUserLoginRepository;
    private final IRolRepository iRolRepository;
    private final PasswordEncoder passwordEncoder;

    public UserLoginService(IUserLoginRepository iUserLoginRepository, IRolRepository iRolRepository, PasswordEncoder passwordEncoder) {
        this.iUserLoginRepository = iUserLoginRepository;
        this.iRolRepository = iRolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserLogin> findAll() {
        return ((List<UserLogin>) this.iUserLoginRepository.findAll()).stream().peek(user -> {

            boolean admin = user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
            user.setAdmin(admin);
        }).collect(Collectors.toList());
    }

    public UserLogin save(UserLogin user) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Verificamos si hay un usuario autenticado
        if (auth != null && auth.isAuthenticated()) {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));

            // Si NO es admin pero quiere crear un admin, no lo permitimos
            if (!isAdmin && user.isAdmin()) {
                throw new AccessDeniedException("Solo los administradores pueden crear otros administradores.");
            }
        } else {
            // Si no está autenticado (registro público), no puede ser admin
            user.setAdmin(false);
        }

        user.setRoles(getRoles(user)); // Asignar roles
        user.setPassword(passwordEncoder.encode(user.getPassword())); // Encriptar contraseña
        return iUserLoginRepository.save(user);
    }


    public Optional<UserLogin> update(UserLogin user, Long id) {
        Optional<UserLogin> userOptional = iUserLoginRepository.findById(id);

        if (userOptional.isPresent()) {
            UserLogin userDb = userOptional.get();
            userDb.setEmail(user.getEmail());
            userDb.setUsername(user.getUsername());
            userDb.setRoles(getRoles(user));

            return Optional.of(iUserLoginRepository.save(userDb));
        }
        return Optional.empty();
    }

    public void delete(Long id) {
        iUserLoginRepository.deleteById(id);
    }

    // Método privado para asignar roles
    private List<Role> getRoles(UserLogin user) {
        List<Role> roles = new ArrayList<>();
        Optional<Role> optionalRoleUser = iRolRepository.findByName("ROLE_USER");
        optionalRoleUser.ifPresent(roles::add);

        // Agregar rol de administrador si aplica
        if (user.isAdmin()) {
            Optional<Role> optionalRoleAdmin = iRolRepository.findByName("ROLE_ADMIN");
            optionalRoleAdmin.ifPresent(roles::add);
        }
        return roles;
    }
}
