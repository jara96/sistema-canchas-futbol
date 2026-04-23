package com.tucancha.backend.config;

import com.tucancha.backend.entity.Rol;
import com.tucancha.backend.enums.RolNombre;
import com.tucancha.backend.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;

    @Override
    public void run(String... args) {
        for (RolNombre nombre : RolNombre.values()) {
            rolRepository.findByNombre(nombre)
                    .orElseGet(() -> rolRepository.save(Rol.builder().nombre(nombre).build()));
        }
    }
}
