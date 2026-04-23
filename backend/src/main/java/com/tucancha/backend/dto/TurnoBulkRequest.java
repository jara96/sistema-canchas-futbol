package com.tucancha.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class TurnoBulkRequest {

    @NotNull
    private LocalTime horaDesde;

    @NotNull
    private LocalTime horaHasta;

    @NotNull
    @Min(15)
    @Max(240)
    private Integer duracionMinutos;
}
