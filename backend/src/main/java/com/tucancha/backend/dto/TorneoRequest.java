package com.tucancha.backend.dto;

import com.tucancha.backend.enums.ModalidadTorneo;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Data
public class TorneoRequest {

    @NotNull
    private Long canchaId;

    @NotNull
    private ModalidadTorneo modalidad;

    /** DIA_ENTERO: única fecha. */
    private LocalDate fecha;

    /** VARIOS_DIAS / HORARIOS_VARIOS_DIAS: lista de fechas. */
    private List<LocalDate> fechas;

    /** HORARIOS_VARIOS_DIAS / RECURRENTE_SEMANAL: lista de turnoIds. */
    private List<Long> turnoIds;

    /** RECURRENTE_SEMANAL. */
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private DayOfWeek diaSemana;
}
