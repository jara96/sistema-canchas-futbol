package com.tucancha.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConfiguracionRequest {

    @NotNull
    @Min(value = 1, message = "El límite de reserva debe ser al menos 1 día")
    @Max(value = 365, message = "El límite de reserva no puede superar 365 días")
    private Integer diasMaximoReserva;

    @NotNull
    @Min(value = 1, message = "El límite de torneo debe ser al menos 1 día")
    @Max(value = 365, message = "El límite de torneo no puede superar 365 días")
    private Integer diasMaximoTorneo;
}
