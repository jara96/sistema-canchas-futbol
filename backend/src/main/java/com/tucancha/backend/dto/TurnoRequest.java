package com.tucancha.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class TurnoRequest {

    @NotNull
    private LocalTime horaInicio;

    @NotNull
    private LocalTime horaFin;

    private Boolean activo;
}
