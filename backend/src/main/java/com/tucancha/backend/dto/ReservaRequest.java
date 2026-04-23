package com.tucancha.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReservaRequest {

    @NotNull
    private Long canchaId;

    @NotNull
    private Long turnoId;

    @NotNull
    private LocalDate fecha;
}
