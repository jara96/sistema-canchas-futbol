package com.tucancha.backend.controller;

import com.tucancha.backend.dto.ReservaRequest;
import com.tucancha.backend.dto.ReservaResponse;
import com.tucancha.backend.security.CurrentUser;
import com.tucancha.backend.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @PostMapping
    public ResponseEntity<ReservaResponse> crear(@Valid @RequestBody ReservaRequest req) {
        return ResponseEntity.ok(ReservaResponse.from(
                reservaService.crear(req, CurrentUser.id())));
    }

    @GetMapping("/mis")
    public List<ReservaResponse> misReservas() {
        return reservaService.listarPorUsuario(CurrentUser.id())
                .stream().map(ReservaResponse::from).toList();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ReservaResponse> listarTodas() {
        return reservaService.listarTodas().stream().map(ReservaResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ReservaResponse obtener(@PathVariable Long id) {
        return ReservaResponse.from(reservaService.obtener(id));
    }

    @GetMapping("/disponibilidad")
    public List<Long> turnosOcupados(@RequestParam Long canchaId,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return reservaService.turnosOcupados(canchaId, fecha);
    }

    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasRole('ADMIN')")
    public ReservaResponse confirmar(@PathVariable Long id) {
        return ReservaResponse.from(reservaService.confirmar(id));
    }

    @PostMapping("/{id}/cancelar")
    public ReservaResponse cancelar(@PathVariable Long id) {
        return ReservaResponse.from(
                reservaService.cancelar(id, CurrentUser.id(), CurrentUser.isAdmin()));
    }
}
