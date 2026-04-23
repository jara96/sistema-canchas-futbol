package com.tucancha.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DiaCerradoRequest {

    @NotNull
    private LocalDate fecha;

    @Size(max = 150)
    private String motivo;
}
