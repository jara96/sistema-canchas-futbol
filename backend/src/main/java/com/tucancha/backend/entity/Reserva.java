package com.tucancha.backend.entity;

import com.tucancha.backend.enums.EstadoReserva;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
// Nota: no usamos unique constraint (cancha, fecha, turno) porque las reservas
// CANCELADA (por expiración o por admin) conviven con una nueva reserva del mismo
// slot. El control de doble-reserva lo hace ReservaRepository.existsOcupado(...)
// a nivel de aplicación, considerando sólo CONFIRMADA + PENDIENTE vigente.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reserva_usuario"))
    private Usuario usuario;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cancha_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reserva_cancha"))
    private Cancha cancha;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turno_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reserva_turno"))
    private Turno turno;

    @NotNull
    @Column(nullable = false)
    private LocalDate fecha;

    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal senia;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoReserva estado = EstadoReserva.PENDIENTE;

    /** Código de 6 dígitos generado al aprobarse la seña.
     *  Se presenta en la cancha para que el encargado cobre el saldo. */
    @Column(name = "codigo_retiro", length = 6)
    private String codigoRetiro;

    /** true cuando el usuario ya pagó el saldo restante (total - seña). */
    @Column(name = "saldo_pagado", nullable = false)
    @Builder.Default
    private Boolean saldoPagado = false;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}
