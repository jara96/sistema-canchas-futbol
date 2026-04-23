package com.tucancha.backend.entity;

import com.tucancha.backend.enums.RolNombre;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_rol_nombre", columnNames = "nombre")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RolNombre nombre;
}
