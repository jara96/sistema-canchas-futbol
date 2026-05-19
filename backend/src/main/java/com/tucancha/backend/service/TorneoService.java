package com.tucancha.backend.service;

import com.tucancha.backend.dto.TorneoRequest;
import com.tucancha.backend.entity.*;
import com.tucancha.backend.enums.EstadoReserva;
import com.tucancha.backend.enums.EstadoTorneo;
import com.tucancha.backend.enums.ModalidadTorneo;
import com.tucancha.backend.exception.BadRequestException;
import com.tucancha.backend.exception.ResourceNotFoundException;
import com.tucancha.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TorneoService {

    private static final int PORCENTAJE_SENIA_DEFAULT = 50;

    private final TorneoRepository torneoRepository;
    private final ReservaRepository reservaRepository;
    private final CanchaRepository canchaRepository;
    private final TurnoRepository turnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DiaCerradoRepository diaCerradoRepository;
    private final ConfiguracionService configuracionService;

    /**
     * Crea un torneo: expande la modalidad en N reservas individuales, valida que
     * todas las fechas estén DESPUÉS del límite normal y DENTRO del límite de torneo,
     * y que ningún turno esté ocupado o cerrado. Si todo ok, crea el Torneo + N reservas
     * en estado PENDIENTE.
     */
    public Torneo crear(TorneoRequest req, Long usuarioId) {
        if (req.getCanchaId() == null) throw new BadRequestException("Falta canchaId");
        if (req.getModalidad() == null) throw new BadRequestException("Falta modalidad");

        Cancha cancha = canchaRepository.findById(req.getCanchaId())
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada"));
        if (Boolean.FALSE.equals(cancha.getActiva())) {
            throw new BadRequestException("La cancha no está activa");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Calcular ventana válida de fechas para torneos
        var config = configuracionService.obtener();
        LocalDate hoy = LocalDate.now();
        LocalDate desde = hoy.plusDays(config.getDiasMaximoReserva() + 1L);
        LocalDate hasta = hoy.plusDays((long) config.getDiasMaximoReserva() + config.getDiasMaximoTorneo());

        // 1) Expandir según modalidad → lista de pares (fecha, turno)
        List<SlotTorneo> slots = expandir(req, cancha);
        if (slots.isEmpty()) {
            throw new BadRequestException("No se generó ningún turno para reservar");
        }

        // 2) Validar cada slot: fecha en rango, día no cerrado, turno libre
        Set<LocalDate> diasCerrados = new HashSet<>();
        for (SlotTorneo s : slots) {
            if (s.fecha.isBefore(desde) || s.fecha.isAfter(hasta)) {
                throw new BadRequestException("La fecha " + s.fecha
                        + " está fuera del rango de torneos (" + desde + " a " + hasta + ")");
            }
            if (diaCerradoRepository.existsByFecha(s.fecha)) {
                diasCerrados.add(s.fecha);
            }
        }
        if (!diasCerrados.isEmpty()) {
            throw new BadRequestException("Las siguientes fechas están cerradas: " + diasCerrados);
        }

        // Validar que no haya turnos ocupados (CONFIRMADA o PENDIENTE vigente)
        LocalDateTime limiteExp = LocalDateTime.now().minusMinutes(ReservaService.MINUTOS_VENTANA_PAGO);
        List<String> conflictos = new ArrayList<>();
        for (SlotTorneo s : slots) {
            if (reservaRepository.existsOcupado(cancha.getId(), s.fecha, s.turno.getId(), limiteExp)) {
                conflictos.add(s.fecha + " " + s.turno.getHoraInicio() + "-" + s.turno.getHoraFin());
            }
        }
        if (!conflictos.isEmpty()) {
            throw new BadRequestException("Algunos turnos ya están reservados: " + conflictos);
        }

        // Validar que no haya duplicados internos (la misma combinación fecha+turno dos veces)
        Set<String> claves = new HashSet<>();
        for (SlotTorneo s : slots) {
            String k = s.fecha + "|" + s.turno.getId();
            if (!claves.add(k)) {
                throw new BadRequestException("Hay turnos duplicados en la solicitud: " + k);
            }
        }

        // 3) Calcular total y seña
        int pct = cancha.getPorcentajeSenia() != null ? cancha.getPorcentajeSenia() : PORCENTAJE_SENIA_DEFAULT;
        BigDecimal totalGlobal = BigDecimal.ZERO;
        BigDecimal seniaGlobal = BigDecimal.ZERO;
        for (SlotTorneo s : slots) {
            s.total = calcularTotal(cancha.getPrecioHora(), s.turno.getHoraInicio(), s.turno.getHoraFin());
            s.senia = s.total.multiply(new BigDecimal(pct))
                    .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
            totalGlobal = totalGlobal.add(s.total);
            seniaGlobal = seniaGlobal.add(s.senia);
        }

        // 4) Persistir torneo y reservas
        Torneo torneo = Torneo.builder()
                .usuario(usuario)
                .cancha(cancha)
                .modalidad(req.getModalidad())
                .estado(EstadoTorneo.PENDIENTE)
                .cantidadTurnos(slots.size())
                .total(totalGlobal)
                .totalSenia(seniaGlobal)
                .fechaCreacion(LocalDateTime.now())
                .build();
        torneo = torneoRepository.save(torneo);

        for (SlotTorneo s : slots) {
            Reserva r = Reserva.builder()
                    .usuario(usuario)
                    .cancha(cancha)
                    .turno(s.turno)
                    .fecha(s.fecha)
                    .total(s.total)
                    .senia(s.senia)
                    .estado(EstadoReserva.PENDIENTE)
                    .torneo(torneo)
                    .build();
            reservaRepository.save(r);
        }

        log.info("Torneo {} creado con {} reservas, total seña ${}", torneo.getId(), slots.size(), seniaGlobal);
        return torneo;
    }

    @Transactional(readOnly = true)
    public Torneo obtener(Long id) {
        return torneoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Torneo no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<Reserva> reservasDelTorneo(Long torneoId) {
        return reservaRepository.findByTorneoId(torneoId);
    }

    @Transactional(readOnly = true)
    public List<Torneo> listarPorUsuario(Long usuarioId) {
        return torneoRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
    }

    /** Cancela los torneos PENDIENTE expirados y sus reservas asociadas. Llamado por scheduler. */
    public int expirarPendientes() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(ReservaService.MINUTOS_VENTANA_PAGO);
        List<Torneo> torneos = torneoRepository.findPorEstadoYAnterioresA(EstadoTorneo.PENDIENTE, limite);
        for (Torneo t : torneos) {
            t.setEstado(EstadoTorneo.EXPIRADO);
            torneoRepository.save(t);
            // Las reservas individuales también las pasamos a CANCELADA
            List<Reserva> reservas = reservaRepository.findByTorneoId(t.getId());
            for (Reserva r : reservas) {
                if (r.getEstado() == EstadoReserva.PENDIENTE) {
                    r.setEstado(EstadoReserva.CANCELADA);
                }
            }
            reservaRepository.saveAll(reservas);
        }
        return torneos.size();
    }

    /* ===================== EXPANSIÓN POR MODALIDAD ===================== */

    private static class SlotTorneo {
        LocalDate fecha;
        Turno turno;
        BigDecimal total;
        BigDecimal senia;

        SlotTorneo(LocalDate f, Turno t) { this.fecha = f; this.turno = t; }
    }

    private List<SlotTorneo> expandir(TorneoRequest req, Cancha cancha) {
        switch (req.getModalidad()) {
            case DIA_ENTERO: return expandirDiaEntero(req);
            case VARIOS_DIAS: return expandirVariosDias(req);
            case HORARIOS_VARIOS_DIAS: return expandirHorariosVariosDias(req);
            case RECURRENTE_SEMANAL: return expandirRecurrenteSemanal(req);
            default: throw new BadRequestException("Modalidad desconocida");
        }
    }

    private List<SlotTorneo> expandirDiaEntero(TorneoRequest req) {
        if (req.getFecha() == null) throw new BadRequestException("Falta fecha");
        List<Turno> turnos = turnoRepository.findByActivoTrueOrderByHoraInicioAsc();
        return turnos.stream().map(t -> new SlotTorneo(req.getFecha(), t)).toList();
    }

    private List<SlotTorneo> expandirVariosDias(TorneoRequest req) {
        if (req.getFechas() == null || req.getFechas().isEmpty()) {
            throw new BadRequestException("Falta lista de fechas");
        }
        List<Turno> turnos = turnoRepository.findByActivoTrueOrderByHoraInicioAsc();
        List<SlotTorneo> out = new ArrayList<>();
        for (LocalDate f : req.getFechas()) {
            for (Turno t : turnos) out.add(new SlotTorneo(f, t));
        }
        return out;
    }

    private List<SlotTorneo> expandirHorariosVariosDias(TorneoRequest req) {
        if (req.getFechas() == null || req.getFechas().isEmpty()) {
            throw new BadRequestException("Falta lista de fechas");
        }
        if (req.getTurnoIds() == null || req.getTurnoIds().isEmpty()) {
            throw new BadRequestException("Falta lista de turnoIds");
        }
        List<Turno> turnos = turnoRepository.findAllById(req.getTurnoIds());
        if (turnos.size() != req.getTurnoIds().size()) {
            throw new BadRequestException("Algún turno no existe");
        }
        List<SlotTorneo> out = new ArrayList<>();
        for (LocalDate f : req.getFechas()) {
            for (Turno t : turnos) {
                if (Boolean.TRUE.equals(t.getActivo())) {
                    out.add(new SlotTorneo(f, t));
                }
            }
        }
        return out;
    }

    private List<SlotTorneo> expandirRecurrenteSemanal(TorneoRequest req) {
        if (req.getFechaInicio() == null || req.getFechaFin() == null) {
            throw new BadRequestException("Falta fechaInicio o fechaFin");
        }
        if (req.getDiaSemana() == null) {
            throw new BadRequestException("Falta diaSemana");
        }
        if (req.getTurnoIds() == null || req.getTurnoIds().isEmpty()) {
            throw new BadRequestException("Falta lista de turnoIds");
        }
        if (req.getFechaFin().isBefore(req.getFechaInicio())) {
            throw new BadRequestException("fechaFin anterior a fechaInicio");
        }
        List<Turno> turnos = turnoRepository.findAllById(req.getTurnoIds());
        if (turnos.size() != req.getTurnoIds().size()) {
            throw new BadRequestException("Algún turno no existe");
        }
        List<SlotTorneo> out = new ArrayList<>();
        LocalDate cursor = req.getFechaInicio();
        // avanzar al primer día que coincida con diaSemana
        while (cursor.getDayOfWeek() != req.getDiaSemana() && !cursor.isAfter(req.getFechaFin())) {
            cursor = cursor.plusDays(1);
        }
        while (!cursor.isAfter(req.getFechaFin())) {
            for (Turno t : turnos) {
                if (Boolean.TRUE.equals(t.getActivo())) {
                    out.add(new SlotTorneo(cursor, t));
                }
            }
            cursor = cursor.plusWeeks(1);
        }
        return out;
    }

    private BigDecimal calcularTotal(BigDecimal precioHora, LocalTime inicio, LocalTime fin) {
        long minutos = java.time.Duration.between(inicio, fin).toMinutes();
        if (minutos <= 0) throw new BadRequestException("Turno con duración inválida");
        BigDecimal horas = new BigDecimal(minutos).divide(new BigDecimal(60), 4, RoundingMode.HALF_UP);
        return precioHora.multiply(horas).setScale(2, RoundingMode.HALF_UP);
    }
}
