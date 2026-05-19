package com.tucancha.backend.controller;

import com.tucancha.backend.dto.ReservaResponse;
import com.tucancha.backend.dto.TorneoRequest;
import com.tucancha.backend.dto.TorneoResponse;
import com.tucancha.backend.entity.Torneo;
import com.tucancha.backend.security.UserPrincipal;
import com.tucancha.backend.service.TorneoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/torneos")
@RequiredArgsConstructor
public class TorneoController {

    private final TorneoService torneoService;

    @PostMapping
    public ResponseEntity<TorneoResponse> crear(@Valid @RequestBody TorneoRequest req,
                                                @AuthenticationPrincipal UserPrincipal user) {
        Torneo t = torneoService.crear(req, user.getId());
        return ResponseEntity.ok(toResponse(t));
    }

    @GetMapping("/{id}")
    public TorneoResponse obtener(@PathVariable Long id,
                                  @AuthenticationPrincipal UserPrincipal user) {
        Torneo t = torneoService.obtener(id);
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!t.getUsuario().getId().equals(user.getId()) && !isAdmin) {
            throw new com.tucancha.backend.exception.BadRequestException("No autorizado");
        }
        return toResponse(t);
    }

    @GetMapping("/mios")
    public List<TorneoResponse> listarMios(@AuthenticationPrincipal UserPrincipal user) {
        return torneoService.listarPorUsuario(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private TorneoResponse toResponse(Torneo t) {
        var reservas = torneoService.reservasDelTorneo(t.getId()).stream()
                .map(ReservaResponse::from)
                .toList();
        return TorneoResponse.builder()
                .id(t.getId())
                .usuarioNombre(t.getUsuario().getNombre())
                .usuarioEmail(t.getUsuario().getEmail())
                .canchaId(t.getCancha().getId())
                .canchaNombre(t.getCancha().getNombre())
                .modalidad(t.getModalidad())
                .estado(t.getEstado())
                .cantidadTurnos(t.getCantidadTurnos())
                .total(t.getTotal())
                .totalSenia(t.getTotalSenia())
                .fechaCreacion(t.getFechaCreacion())
                .reservas(reservas)
                .build();
    }
}
