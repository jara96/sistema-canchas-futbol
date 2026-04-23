package com.tucancha.backend.service;

import com.tucancha.backend.dto.TurnoBulkRequest;
import com.tucancha.backend.dto.TurnoRequest;
import com.tucancha.backend.entity.Turno;
import com.tucancha.backend.exception.BadRequestException;
import com.tucancha.backend.exception.ResourceNotFoundException;
import com.tucancha.backend.repository.ReservaRepository;
import com.tucancha.backend.repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TurnoService {

    private final TurnoRepository turnoRepository;
    private final ReservaRepository reservaRepository;

    @Transactional(readOnly = true)
    public List<Turno> listarActivos() {
        return turnoRepository.findByActivoTrueOrderByHoraInicioAsc();
    }

    @Transactional(readOnly = true)
    public List<Turno> listarTodos() {
        return turnoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Turno obtener(Long id) {
        return turnoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado: " + id));
    }

    public Turno crear(TurnoRequest req) {
        Turno t = Turno.builder()
                .horaInicio(req.getHoraInicio())
                .horaFin(req.getHoraFin())
                .activo(req.getActivo() == null ? Boolean.TRUE : req.getActivo())
                .build();
        return turnoRepository.save(t);
    }

    public Turno actualizar(Long id, TurnoRequest req) {
        Turno t = obtener(id);
        t.setHoraInicio(req.getHoraInicio());
        t.setHoraFin(req.getHoraFin());
        if (req.getActivo() != null) t.setActivo(req.getActivo());
        return turnoRepository.save(t);
    }

    /**
     * Si el turno tiene reservas, se archiva (activo=false); si no, se borra.
     */
    public boolean eliminar(Long id) {
        Turno t = obtener(id);
        long reservas = reservaRepository.countByTurnoId(id);
        if (reservas == 0) {
            turnoRepository.delete(t);
            return true;
        }
        t.setActivo(false);
        turnoRepository.save(t);
        return false;
    }

    /**
     * Genera turnos consecutivos desde horaDesde hasta horaHasta con la duración indicada.
     * Los que ya existan (mismo horaInicio/horaFin) se omiten por el unique constraint.
     */
    public List<Turno> crearBulk(TurnoBulkRequest req) {
        if (!req.getHoraDesde().isBefore(req.getHoraHasta())) {
            throw new BadRequestException("horaDesde debe ser anterior a horaHasta");
        }

        List<Turno> existentes = turnoRepository.findAll();
        List<Turno> creados = new ArrayList<>();

        LocalTime inicio = req.getHoraDesde();
        while (inicio.plusMinutes(req.getDuracionMinutos()).isAfter(inicio)
                && !inicio.plusMinutes(req.getDuracionMinutos()).isAfter(req.getHoraHasta())) {

            LocalTime fin = inicio.plusMinutes(req.getDuracionMinutos());
            final LocalTime i = inicio, f = fin;
            boolean yaExiste = existentes.stream()
                    .anyMatch(t -> t.getHoraInicio().equals(i) && t.getHoraFin().equals(f));

            if (!yaExiste) {
                creados.add(turnoRepository.save(Turno.builder()
                        .horaInicio(inicio).horaFin(fin).activo(true).build()));
            }
            inicio = fin;
        }
        return creados;
    }
}
