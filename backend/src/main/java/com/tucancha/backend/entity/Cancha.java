package com.tucancha.backend.entity;

import com.tucancha.backend.enums.TipoCancha;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "canchas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cancha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoCancha tipo;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "precio_hora", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioHora;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    @Column(name = "porcentaje_senia")
    @Builder.Default
    private Integer porcentajeSenia = 50;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}
