package com.tucancha.backend.security.oauth2;

import com.tucancha.backend.entity.Rol;
import com.tucancha.backend.entity.Usuario;
import com.tucancha.backend.enums.AuthProvider;
import com.tucancha.backend.enums.RolNombre;
import com.tucancha.backend.exception.BadRequestException;
import com.tucancha.backend.repository.RolRepository;
import com.tucancha.backend.repository.UsuarioRepository;
import com.tucancha.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest req) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(req);
        try {
            return process(req, oAuth2User);
        } catch (Exception ex) {
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User process(OAuth2UserRequest req, OAuth2User oAuth2User) {
        Map<String, Object> attrs = oAuth2User.getAttributes();
        String providerId = (String) attrs.get("sub");
        String email = (String) attrs.get("email");
        String name = (String) attrs.get("name");

        if (email == null || email.isBlank()) {
            throw new BadRequestException("El proveedor OAuth2 no devolvió email");
        }

        AuthProvider provider = AuthProvider.valueOf(
                req.getClientRegistration().getRegistrationId().toUpperCase());

        Usuario usuario = usuarioRepository.findByEmail(email)
                .map(u -> actualizar(u, provider, providerId, name))
                .orElseGet(() -> registrar(provider, providerId, email, name));

        return UserPrincipal.create(usuario, attrs);
    }

    private Usuario registrar(AuthProvider provider, String providerId, String email, String nombre) {
        Rol rolUser = rolRepository.findByNombre(RolNombre.USER)
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre(RolNombre.USER).build()));

        Set<Rol> roles = new HashSet<>();
        roles.add(rolUser);

        Usuario u = Usuario.builder()
                .nombre(nombre == null ? email : nombre)
                .email(email)
                .provider(provider)
                .providerId(providerId)
                .activo(true)
                .roles(roles)
                .build();
        return usuarioRepository.save(u);
    }

    private Usuario actualizar(Usuario u, AuthProvider provider, String providerId, String nombre) {
        if (u.getProvider() != provider) {
            throw new BadRequestException("La cuenta ya existe con otro proveedor: " + u.getProvider());
        }
        u.setProviderId(providerId);
        if (nombre != null) u.setNombre(nombre);
        return usuarioRepository.save(u);
    }
}
