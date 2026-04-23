package com.tucancha.backend.repository;

import com.tucancha.backend.entity.Usuario;
import com.tucancha.backend.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByProviderAndProviderId(AuthProvider provider, String providerId);

    boolean existsByEmail(String email);
}
