package com.tucancha.backend.service;

import com.tucancha.backend.dto.auth.AuthResponse;
import com.tucancha.backend.dto.auth.LoginRequest;
import com.tucancha.backend.dto.auth.RegisterRequest;
import com.tucancha.backend.entity.Rol;
import com.tucancha.backend.entity.Usuario;
import com.tucancha.backend.enums.AuthProvider;
import com.tucancha.backend.enums.RolNombre;
import com.tucancha.backend.exception.BadRequestException;
import com.tucancha.backend.repository.RolRepository;
import com.tucancha.backend.repository.UsuarioRepository;
import com.tucancha.backend.security.JwtService;
import com.tucancha.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest req) {
        if (usuarioRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("El email ya está registrado");
        }

        Rol rolUser = rolRepository.findByNombre(RolNombre.USER)
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre(RolNombre.USER).build()));

        Set<Rol> roles = new HashSet<>();
        roles.add(rolUser);

        Usuario u = Usuario.builder()
                .nombre(req.getNombre())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .provider(AuthProvider.LOCAL)
                .activo(true)
                .roles(roles)
                .build();

        usuarioRepository.save(u);

        UserPrincipal principal = UserPrincipal.create(u);
        return buildAuth(principal, jwtService.generateToken(principal));
    }

    public AuthResponse login(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        return buildAuth(principal, jwtService.generateToken(principal));
    }

    private AuthResponse buildAuth(UserPrincipal p, String token) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .id(p.getId())
                .email(p.getEmail())
                .nombre(p.getNombre())
                .roles(p.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .collect(Collectors.toSet()))
                .build();
    }
}
