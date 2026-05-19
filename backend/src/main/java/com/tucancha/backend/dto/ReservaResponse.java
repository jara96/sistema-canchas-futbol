package com.tucancha.backend.dto;

import com.tucancha.backend.entity.Reserva;
import com.tucancha.backend.enums.EstadoReserva;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservaResponse {
    private Long id;
    private Long canchaId;
    private String canchaNombre;
    private Long turnoId;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private LocalDate fecha;
    private BigDecimal total;
    private BigDecimal senia;
    private BigDecimal saldo;
    private EstadoReserva estado;
    private Long usuarioId;
    private String usuarioNombre;
    private String usuarioEmail;
    private String codigoRetiro;
    private Boolean saldoPagado;
    private Long torneoId;

    public static ReservaResponse from(Reserva r) {
        BigDecimal total = r.getTotal();
        BigDecimal senia = r.getSenia();
        BigDecimal saldo = total != null && senia != null ? total.subtract(senia) : null;
        return ReservaResponse.builder()
                .id(r.getId())
                .canchaId(r.getCancha().getId())
                .canchaNombre(r.getCancha().getNombre())
                .turnoId(r.getTurno().getId())
                .horaInicio(r.getTurno().getHoraInicio())
                .horaFin(r.getTurno().getHoraFin())
                .fecha(r.getFecha())
                .total(total)
                .senia(senia)
                .saldo(saldo)
                .estado(r.getEstado())
                .usuarioId(r.getUsuario().getId())
                .usuarioNombre(r.getUsuario().getNombre())
                .usuarioEmail(r.getUsuario().getEmail())
                .codigoRetiro(r.getCodigoRetiro())
                .saldoPagado(Boolean.TRUE.equals(r.getSaldoPagado()))
                .torneoId(r.getTorneo() != null ? r.getTorneo().getId() : null)
                .build();
    }
}
