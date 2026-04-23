package com.tucancha.backend.security;

import com.tucancha.backend.entity.Usuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class UserPrincipal implements UserDetails, OAuth2User {

    private final Long id;
    private final String email;
    private final String password;
    private final String nombre;
    private final Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;

    public UserPrincipal(Long id, String email, String password, String nombre,
                         Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.nombre = nombre;
        this.authorities = authorities;
    }

    public static UserPrincipal create(Usuario u) {
        Set<GrantedAuthority> authorities = u.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getNombre().name()))
                .collect(Collectors.toSet());
        return new UserPrincipal(u.getId(), u.getEmail(),
                u.getPassword() == null ? "" : u.getPassword(),
                u.getNombre(), authorities);
    }

    public static UserPrincipal create(Usuario u, Map<String, Object> attributes) {
        UserPrincipal p = create(u);
        p.attributes = attributes;
        return p;
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
    @Override public Map<String, Object> getAttributes() { return attributes; }
    @Override public String getName() { return String.valueOf(id); }
}
