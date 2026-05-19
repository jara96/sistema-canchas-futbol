package com.tucancha.backend.dto;

import com.tucancha.backend.enums.EstadoTorneo;
import com.tucancha.backend.enums.ModalidadTorneo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TorneoResponse {
    private Long id;
    private String usuarioNombre;
    private String usuarioEmail;
    private Long canchaId;
    private String canchaNombre;
    private ModalidadTorneo modalidad;
    private EstadoTorneo estado;
    private Integer cantidadTurnos;
    private BigDecimal total;
    private BigDecimal totalSenia;
    private LocalDateTime fechaCreacion;
    private List<ReservaResponse> reservas;
}
