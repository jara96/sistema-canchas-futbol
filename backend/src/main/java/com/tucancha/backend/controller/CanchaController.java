package com.tucancha.backend.controller;

import com.tucancha.backend.dto.CanchaRequest;
import com.tucancha.backend.entity.Cancha;
import com.tucancha.backend.service.CanchaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/canchas")
@RequiredArgsConstructor
public class CanchaController {

    private final CanchaService canchaService;

    @GetMapping("/publicas")
    public List<Cancha> listarActivas() {
        return canchaService.listarActivas();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Cancha> listarTodas() {
        return canchaService.listarTodas();
    }

    @GetMapping("/{id}")
    public Cancha obtener(@PathVariable Long id) {
        return canchaService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Cancha> crear(@Valid @RequestBody CanchaRequest req) {
        return ResponseEntity.ok(canchaService.crear(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Cancha actualizar(@PathVariable Long id, @Valid @RequestBody CanchaRequest req) {
        return canchaService.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> eliminar(@PathVariable Long id) {
        boolean borrada = canchaService.eliminar(id);
        return ResponseEntity.ok(java.util.Map.of(
                "deleted", borrada,
                "archived", !borrada,
                "message", borrada ? "Cancha eliminada" : "Tenía reservas: se archivó (inactiva)"
        ));
    }
}
