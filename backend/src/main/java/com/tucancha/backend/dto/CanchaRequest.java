package com.tucancha.backend.dto;

import com.tucancha.backend.enums.TipoCancha;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CanchaRequest {

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @NotNull
    private TipoCancha tipo;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal precioHora;

    private Boolean activa;

    @jakarta.validation.constraints.Min(1)
    @jakarta.validation.constraints.Max(100)
    private Integer porcentajeSenia;
}
