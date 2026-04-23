package com.tucancha.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "turnos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_turno_horario", columnNames = {"hora_inicio", "hora_fin"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @NotNull
    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
