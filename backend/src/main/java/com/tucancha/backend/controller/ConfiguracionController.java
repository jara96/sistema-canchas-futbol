package com.tucancha.backend.controller;

import com.tucancha.backend.dto.ConfiguracionRequest;
import com.tucancha.backend.entity.ConfiguracionSistema;
import com.tucancha.backend.service.ConfiguracionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfiguracionService service;

    /** Endpoint público para que el frontend conozca los límites de fecha. */
    @GetMapping("/publico")
    public ConfiguracionSistema obtenerPublico() {
        return service.obtener();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ConfiguracionSistema obtener() {
        return service.obtener();
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ConfiguracionSistema actualizar(@Valid @RequestBody ConfiguracionRequest req) {
        return service.actualizar(req);
    }
}
