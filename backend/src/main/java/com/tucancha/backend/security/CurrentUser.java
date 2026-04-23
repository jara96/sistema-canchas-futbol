package com.tucancha.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {}

    public static UserPrincipal get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal up)) {
            throw new IllegalStateException("Usuario no autenticado");
        }
        return up;
    }

    public static Long id() {
        return get().getId();
    }

    public static boolean isAdmin() {
        return get().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
