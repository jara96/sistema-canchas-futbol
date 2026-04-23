package com.tucancha.backend.controller;

import com.tucancha.backend.dto.TurnoBulkRequest;
import com.tucancha.backend.dto.TurnoRequest;
import com.tucancha.backend.entity.Turno;
import com.tucancha.backend.service.TurnoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoService turnoService;

    @GetMapping("/publicos")
    public List<Turno> listarActivos() {
        return turnoService.listarActivos();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Turno> listarTodos() {
        return turnoService.listarTodos();
    }

    @GetMapping("/{id}")
    public Turno obtener(@PathVariable Long id) {
        return turnoService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Turno> crear(@Valid @RequestBody TurnoRequest req) {
        return ResponseEntity.ok(turnoService.crear(req));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Turno> crearBulk(@Valid @RequestBody TurnoBulkRequest req) {
        return turnoService.crearBulk(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Turno actualizar(@PathVariable Long id, @Valid @RequestBody TurnoRequest req) {
        return turnoService.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> eliminar(@PathVariable Long id) {
        boolean borrado = turnoService.eliminar(id);
        return ResponseEntity.ok(java.util.Map.of(
                "deleted", borrado,
                "archived", !borrado,
                "message", borrado ? "Turno eliminado" : "Tenía reservas: se archivó (inactivo)"
        ));
    }
}
