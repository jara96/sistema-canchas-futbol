package com.tucancha.backend.controller;

import com.tucancha.backend.dto.DiaCerradoRequest;
import com.tucancha.backend.entity.DiaCerrado;
import com.tucancha.backend.service.DiaCerradoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dias-cerrados")
@RequiredArgsConstructor
public class DiaCerradoController {

    private final DiaCerradoService service;

    @GetMapping("/publicos")
    public List<DiaCerrado> publicos() {
        return service.listarProximos();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<DiaCerrado> listar() {
        return service.listar();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DiaCerrado> crear(@Valid @RequestBody DiaCerradoRequest req) {
        return ResponseEntity.ok(service.crear(req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
