package com.tucancha.backend.repository;

import com.tucancha.backend.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    List<Reserva> findByCanchaIdAndFecha(Long canchaId, LocalDate fecha);

    long countByCanchaId(Long canchaId);

    long countByTurnoId(Long turnoId);

    /**
     * Turno ocupado = CONFIRMADA, o PENDIENTE que todavía no expiró (dentro de la ventana de pago).
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END FROM Reserva r " +
            "WHERE r.cancha.id = :canchaId AND r.fecha = :fecha AND r.turno.id = :turnoId " +
            "AND (r.estado = com.tucancha.backend.enums.EstadoReserva.CONFIRMADA " +
            "     OR (r.estado = com.tucancha.backend.enums.EstadoReserva.PENDIENTE " +
            "         AND r.fechaCreacion >= :limiteExpiracion))")
    boolean existsOcupado(@Param("canchaId") Long canchaId,
                          @Param("fecha") LocalDate fecha,
                          @Param("turnoId") Long turnoId,
                          @Param("limiteExpiracion") LocalDateTime limiteExpiracion);

    @Query("SELECT r.turno.id FROM Reserva r " +
            "WHERE r.cancha.id = :canchaId AND r.fecha = :fecha " +
            "AND (r.estado = com.tucancha.backend.enums.EstadoReserva.CONFIRMADA " +
            "     OR (r.estado = com.tucancha.backend.enums.EstadoReserva.PENDIENTE " +
            "         AND r.fechaCreacion >= :limiteExpiracion))")
    List<Long> findTurnosOcupados(@Param("canchaId") Long canchaId,
                                  @Param("fecha") LocalDate fecha,
                                  @Param("limiteExpiracion") LocalDateTime limiteExpiracion);

    /** PENDIENTE con fechaCreacion anterior al límite: expiraron sin pago. */
    @Query("SELECT r FROM Reserva r WHERE r.estado = com.tucancha.backend.enums.EstadoReserva.PENDIENTE " +
            "AND r.fechaCreacion < :limite")
    List<Reserva> findPendientesExpiradas(@Param("limite") LocalDateTime limite);

    /** Reservas cuya fecha de juego ya pasó y siguen activas (PENDIENTE o CONFIRMADA). */
    @Query("SELECT r FROM Reserva r WHERE r.fecha < :hoy " +
            "AND (r.estado = com.tucancha.backend.enums.EstadoReserva.CONFIRMADA " +
            "  OR r.estado = com.tucancha.backend.enums.EstadoReserva.PENDIENTE)")
    List<Reserva> findPasadasActivas(@Param("hoy") LocalDate hoy);

    Optional<Reserva> findByCodigoRetiro(String codigoRetiro);

    boolean existsByCodigoRetiro(String codigoRetiro);

    List<Reserva> findByTorneoId(Long torneoId);
}
