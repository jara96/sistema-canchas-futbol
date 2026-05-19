package com.tucancha.backend.entity;

import com.tucancha.backend.enums.EstadoTorneo;
import com.tucancha.backend.enums.ModalidadTorneo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "torneos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Torneo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cancha_id", nullable = false)
    private Cancha cancha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ModalidadTorneo modalidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoTorneo estado = EstadoTorneo.PENDIENTE;

    /** Cantidad de turnos reservados dentro de este torneo. */
    @Column(name = "cantidad_turnos", nullable = false)
    @Builder.Default
    private Integer cantidadTurnos = 0;

    /** Total a pagar entre todos los turnos del torneo. */
    @Column(name = "total", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    /** Seña total a pagar (suma de las señas individuales). */
    @Column(name = "total_senia", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalSenia = BigDecimal.ZERO;

    @Column(name = "fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
