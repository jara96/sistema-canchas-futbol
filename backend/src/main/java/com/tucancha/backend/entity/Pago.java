package com.tucancha.backend.entity;

import com.tucancha.backend.enums.EstadoPago;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos", indexes = {
        @Index(name = "idx_pago_mp_payment_id", columnList = "mp_payment_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pago_reserva"))
    private Reserva reserva;

    @Size(max = 100)
    @Column(name = "mp_payment_id", length = 100)
    private String mpPaymentId;

    @Size(max = 100)
    @Column(name = "mp_preference_id", length = 100)
    private String mpPreferenceId;

    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoPago estado = EstadoPago.PENDIENTE;

    @Size(max = 50)
    @Column(name = "metodo_pago", length = 50)
    private String metodoPago;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}
