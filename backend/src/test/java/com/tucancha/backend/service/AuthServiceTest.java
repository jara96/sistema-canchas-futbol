package com.tucancha.backend.service;

import com.tucancha.backend.dto.auth.AuthResponse;
import com.tucancha.backend.dto.auth.RegisterRequest;
import com.tucancha.backend.entity.Rol;
import com.tucancha.backend.entity.Usuario;
import com.tucancha.backend.enums.RolNombre;
import com.tucancha.backend.exception.BadRequestException;
import com.tucancha.backend.repository.RolRepository;
import com.tucancha.backend.repository.UsuarioRepository;
import com.tucancha.backend.security.JwtService;
import com.tucancha.backend.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthService authService;

    @Test
    void register_devuelveTokenYUsuarioCreado() {
        RegisterRequest req = new RegisterRequest();
        req.setNombre("Juan");
        req.setEmail("juan@mail.com");
        req.setPassword("123456");

        when(usuarioRepository.existsByEmail("juan@mail.com")).thenReturn(false);
        when(rolRepository.findByNombre(RolNombre.USER))
                .thenReturn(Optional.of(Rol.builder().id(1L).nombre(RolNombre.USER).build()));
        when(passwordEncoder.encode("123456")).thenReturn("HASH");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("jwt-token");

        AuthResponse resp = authService.register(req);

        assertThat(resp.getToken()).isEqualTo("jwt-token");
        assertThat(resp.getEmail()).isEqualTo("juan@mail.com");
        assertThat(resp.getRoles()).contains("ROLE_USER");
    }

    @Test
    void register_falla_siEmailExiste() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("dup@mail.com");
        req.setPassword("123456");
        req.setNombre("X");

        when(usuarioRepository.existsByEmail("dup@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ya está registrado");
    }
}
