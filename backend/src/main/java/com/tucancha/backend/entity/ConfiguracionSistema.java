package com.tucancha.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuración global del sistema. Tabla con una sola fila (id = 1).
 */
@Entity
@Table(name = "configuracion_sistema")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionSistema {

    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id;

    /** Días máximos hacia el futuro que un usuario común puede reservar (default 30). */
    @Column(name = "dias_maximo_reserva", nullable = false)
    @Builder.Default
    private Integer diasMaximoReserva = 30;

    /**
     * Días máximos adicionales (después del límite normal) en los que se permiten
     * reservas de torneo. Ejemplo: si diasMaximoReserva = 30 y diasMaximoTorneo = 90,
     * las reservas comunes son del día 1 al 30, y los torneos del 31 al 120.
     */
    @Column(name = "dias_maximo_torneo", nullable = false)
    @Builder.Default
    private Integer diasMaximoTorneo = 90;
}
