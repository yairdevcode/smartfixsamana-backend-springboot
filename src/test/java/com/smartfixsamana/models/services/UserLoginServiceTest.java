package com.smartfixsamana.models.services;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.smartfixsamana.models.entities.Role;
import com.smartfixsamana.models.entities.UserLogin;
import com.smartfixsamana.models.repositories.IRolRepository;
import com.smartfixsamana.models.repositories.IUserLoginRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Caso AUT-006 del plan de pruebas GA9-220501096-AA1-EV02.
 *
 * Verifica que la contraseña nunca se almacene en texto plano, que el valor
 * persistido sea verificable con BCrypt y que el registro público no permita
 * crear usuarios con privilegios de administrador.
 *
 * Se usa un BCryptPasswordEncoder real y no un simulacro, porque lo que
 * interesa comprobar es el cifrado efectivo y no que se invoque un método.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserLoginService - almacenamiento cifrado de la contraseña")
class UserLoginServiceTest {

    private static final String CLAVE_EN_TEXTO_PLANO = "ClaveDePrueba2026";

    @Mock
    private IUserLoginRepository userLoginRepository;

    @Mock
    private IRolRepository rolRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserLoginService service;

    @BeforeEach
    void setUp() {
        service = new UserLoginService(userLoginRepository, rolRepository, passwordEncoder);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserLogin nuevoUsuario() {
        UserLogin usuario = new UserLogin();
        usuario.setUsername("tecnico.prueba");
        usuario.setEmail("Tecnico.Prueba@Example.com");
        usuario.setPassword(CLAVE_EN_TEXTO_PLANO);
        return usuario;
    }

    private void sinDuplicadosYConRolUsuario() {
        when(userLoginRepository.existsByUsername(any())).thenReturn(false);
        when(userLoginRepository.existsByEmailIgnoreCase(any())).thenReturn(false);

        Role rolUsuario = new Role();
        rolUsuario.setId(1L);
        rolUsuario.setName("ROLE_USER");
        when(rolRepository.findByName("ROLE_USER")).thenReturn(Optional.of(rolUsuario));

        when(userLoginRepository.save(any(UserLogin.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private UserLogin capturarUsuarioGuardado() {
        ArgumentCaptor<UserLogin> capturado = ArgumentCaptor.forClass(UserLogin.class);
        org.mockito.Mockito.verify(userLoginRepository).save(capturado.capture());
        return capturado.getValue();
    }

    // ---------------------------------------------------------------- AUT-006

    @Test
    @DisplayName("AUT-006: la contraseña almacenada no coincide con el texto plano")
    void laClaveAlmacenadaNoEsElTextoPlano() {
        sinDuplicadosYConRolUsuario();

        service.save(nuevoUsuario());

        UserLogin guardado = capturarUsuarioGuardado();
        assertThat(guardado.getPassword()).isNotEqualTo(CLAVE_EN_TEXTO_PLANO);
        assertThat(guardado.getPassword()).doesNotContain(CLAVE_EN_TEXTO_PLANO);
    }

    @Test
    @DisplayName("AUT-006: la contraseña almacenada tiene el formato de un hash BCrypt")
    void laClaveAlmacenadaTieneFormatoBCrypt() {
        sinDuplicadosYConRolUsuario();

        service.save(nuevoUsuario());

        String almacenada = capturarUsuarioGuardado().getPassword();
        assertThat(almacenada).startsWith("$2");
        assertThat(almacenada).hasSize(60);
    }

    @Test
    @DisplayName("AUT-006: la contraseña original se verifica correctamente contra el hash")
    void laClaveOriginalSeVerificaContraElHash() {
        sinDuplicadosYConRolUsuario();

        service.save(nuevoUsuario());

        String almacenada = capturarUsuarioGuardado().getPassword();
        assertThat(passwordEncoder.matches(CLAVE_EN_TEXTO_PLANO, almacenada)).isTrue();
        assertThat(passwordEncoder.matches("otraClaveDistinta", almacenada)).isFalse();
    }

    @Test
    @DisplayName("AUT-006: dos usuarios con la misma contraseña producen hashes distintos")
    void dosUsuariosConLaMismaClaveProducenHashesDistintos() {
        sinDuplicadosYConRolUsuario();

        UserLogin primero = service.save(nuevoUsuario());
        String hashPrimero = primero.getPassword();

        UserLogin segundoUsuario = nuevoUsuario();
        segundoUsuario.setUsername("tecnico.prueba2");
        segundoUsuario.setEmail("tecnico.prueba2@example.com");
        String hashSegundo = service.save(segundoUsuario).getPassword();

        assertThat(hashPrimero).isNotEqualTo(hashSegundo);
        assertThat(passwordEncoder.matches(CLAVE_EN_TEXTO_PLANO, hashPrimero)).isTrue();
        assertThat(passwordEncoder.matches(CLAVE_EN_TEXTO_PLANO, hashSegundo)).isTrue();
    }

    // ------------------------------------------------- registro sin sesión

    @Test
    @DisplayName("AUT-006: el registro sin sesión activa no puede crear un administrador")
    void elRegistroSinSesionNoCreaAdministradores() {
        sinDuplicadosYConRolUsuario();

        UserLogin aspirante = nuevoUsuario();
        aspirante.setAdmin(true);

        service.save(aspirante);

        assertThat(capturarUsuarioGuardado().isAdmin()).isFalse();
    }

    @Test
    @DisplayName("AUT-006: el correo se normaliza antes de almacenarse")
    void elCorreoSeNormalizaAntesDeAlmacenarse() {
        sinDuplicadosYConRolUsuario();

        service.save(nuevoUsuario());

        assertThat(capturarUsuarioGuardado().getEmail()).isEqualTo("tecnico.prueba@example.com");
    }

    @Test
    @DisplayName("AUT-006: al usuario nuevo se le asigna el rol de usuario")
    void alUsuarioNuevoSeLeAsignaElRolDeUsuario() {
        sinDuplicadosYConRolUsuario();

        service.save(nuevoUsuario());

        List<Role> roles = capturarUsuarioGuardado().getRoles();
        assertThat(roles).extracting(Role::getName).containsExactly("ROLE_USER");
    }
}
