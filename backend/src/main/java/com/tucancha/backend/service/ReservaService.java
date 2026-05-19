package com.tucancha.backend.service;

import com.tucancha.backend.dto.ReservaRequest;
import com.tucancha.backend.entity.Cancha;
import com.tucancha.backend.entity.Reserva;
import com.tucancha.backend.entity.Turno;
import com.tucancha.backend.entity.Usuario;
import com.tucancha.backend.enums.EstadoReserva;
import com.tucancha.backend.exception.BadRequestException;
import com.tucancha.backend.exception.ResourceNotFoundException;
import com.tucancha.backend.repository.CanchaRepository;
import com.tucancha.backend.repository.DiaCerradoRepository;
import com.tucancha.backend.repository.ReservaRepository;
import com.tucancha.backend.repository.TurnoRepository;
import com.tucancha.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservaService {

    private static final int PORCENTAJE_SENIA_DEFAULT = 50;
    /** Minutos que una reserva PENDIENTE mantiene bloqueado el turno antes de expirar. */
    public static final int MINUTOS_VENTANA_PAGO = 10;

    private final ReservaRepository reservaRepository;
    private final CanchaRepository canchaRepository;
    private final TurnoRepository turnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DiaCerradoRepository diaCerradoRepository;
    private final ConfiguracionService configuracionService;

    private LocalDateTime limiteExpiracion() {
        return LocalDateTime.now().minusMinutes(MINUTOS_VENTANA_PAGO);
    }

    public Reserva crear(ReservaRequest req, Long usuarioId) {
        if (req.getFecha().isBefore(LocalDate.now())) {
            throw new BadRequestException("La fecha no puede ser anterior a hoy");
        }

        int diasMaximo = configuracionService.obtener().getDiasMaximoReserva();
        LocalDate limiteFecha = LocalDate.now().plusDays(diasMaximo);
        if (req.getFecha().isAfter(limiteFecha)) {
            throw new BadRequestException("Solo se permiten reservas hasta el " + limiteFecha
                    + ". Para fechas posteriores usá la opción de torneo.");
        }

        if (diaCerradoRepository.existsByFecha(req.getFecha())) {
            throw new BadRequestException("Ese día las canchas están cerradas");
        }

        Cancha cancha = canchaRepository.findById(req.getCanchaId())
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada"));

        if (Boolean.FALSE.equals(cancha.getActiva())) {
            throw new BadRequestException("La cancha no está activa");
        }

        Turno turno = turnoRepository.findById(req.getTurnoId())
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado"));

        if (Boolean.FALSE.equals(turno.getActivo())) {
            throw new BadRequestException("El turno no está activo");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        boolean ocupado = reservaRepository
                .existsOcupado(cancha.getId(), req.getFecha(), turno.getId(), limiteExpiracion());
        if (ocupado) {
            throw new BadRequestException("Ese turno ya está reservado o con pago en curso");
        }

        BigDecimal total = calcularTotal(cancha.getPrecioHora(), turno.getHoraInicio(), turno.getHoraFin());
        int pct = cancha.getPorcentajeSenia() != null ? cancha.getPorcentajeSenia() : PORCENTAJE_SENIA_DEFAULT;
        BigDecimal senia = total.multiply(new BigDecimal(pct))
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);

        Reserva reserva = Reserva.builder()
                .usuario(usuario)
                .cancha(cancha)
                .turno(turno)
                .fecha(req.getFecha())
                .total(total)
                .senia(senia)
                .estado(EstadoReserva.PENDIENTE)
                .build();

        return reservaRepository.save(reserva);
    }

    @Transactional(readOnly = true)
    public Reserva obtener(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarPorUsuario(Long usuarioId) {
        return reservaRepository.findByUsuarioIdOrderByFechaDesc(usuarioId);
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarTodas() {
        return reservaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Long> turnosOcupados(Long canchaId, LocalDate fecha) {
        return reservaRepository.findTurnosOcupados(canchaId, fecha, limiteExpiracion());
    }

    /**
     * Cancela automáticamente las reservas PENDIENTE que superaron la ventana de pago.
     * Corre cada minuto por {@link com.tucancha.backend.config.SchedulerConfig}.
     */
    public int expirarPendientes() {
        List<Reserva> expiradas = reservaRepository.findPendientesExpiradas(limiteExpiracion());
        for (Reserva r : expiradas) {
            r.setEstado(EstadoReserva.CANCELADA);
        }
        if (!expiradas.isEmpty()) reservaRepository.saveAll(expiradas);
        return expiradas.size();
    }

    /**
     * Finaliza/cancela reservas cuya fecha ya pasó (al cruzar 00:00 del día siguiente):
     *  - CONFIRMADA con fecha &lt; hoy → FINALIZADA (el cliente vino o tuvo derecho).
     *  - PENDIENTE con fecha &lt; hoy → CANCELADA (nunca se concretó).
     * Llamado por el scheduler horario.
     */
    public int finalizarPasadas() {
        LocalDate hoy = LocalDate.now();
        List<Reserva> pasadas = reservaRepository.findPasadasActivas(hoy);
        int count = 0;
        for (Reserva r : pasadas) {
            if (r.getEstado() == EstadoReserva.CONFIRMADA) {
                r.setEstado(EstadoReserva.FINALIZADA);
                count++;
            } else if (r.getEstado() == EstadoReserva.PENDIENTE) {
                r.setEstado(EstadoReserva.CANCELADA);
                count++;
            }
        }
        if (count > 0) reservaRepository.saveAll(pasadas);
        return count;
    }

    public Reserva confirmar(Long id) {
        Reserva r = obtener(id);
        r.setEstado(EstadoReserva.CONFIRMADA);
        return reservaRepository.save(r);
    }

    public Reserva cancelar(Long id, Long usuarioId, boolean isAdmin) {
        Reserva r = obtener(id);
        if (!isAdmin && !r.getUsuario().getId().equals(usuarioId)) {
            throw new BadRequestException("No podés cancelar una reserva ajena");
        }
        if (r.getEstado() == EstadoReserva.FINALIZADA) {
            throw new BadRequestException("No se puede cancelar una reserva ya finalizada");
        }
        if (r.getEstado() == EstadoReserva.CANCELADA) {
            throw new BadRequestException("La reserva ya está cancelada");
        }
        r.setEstado(EstadoReserva.CANCELADA);
        return reservaRepository.save(r);
    }

    private BigDecimal calcularTotal(BigDecimal precioHora, LocalTime inicio, LocalTime fin) {
        long minutos = java.time.Duration.between(inicio, fin).toMinutes();
        if (minutos <= 0) {
            throw new BadRequestException("El turno tiene una duración inválida");
        }
        BigDecimal horas = new BigDecimal(minutos).divide(new BigDecimal(60), 4, RoundingMode.HALF_UP);
        return precioHora.multiply(horas).setScale(2, RoundingMode.HALF_UP);
    }
}
