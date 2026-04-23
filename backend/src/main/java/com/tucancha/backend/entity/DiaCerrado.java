package com.tucancha.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "dias_cerrados", uniqueConstraints = {
        @UniqueConstraint(name = "uk_dia_cerrado_fecha", columnNames = "fecha")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaCerrado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false, unique = true)
    private LocalDate fecha;

    @Size(max = 150)
    @Column(length = 150)
    private String motivo;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}
