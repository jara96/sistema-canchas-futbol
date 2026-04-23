package com.tucancha.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciaResponse {
    private String preferenceId;
    private String initPoint;
    private String sandboxInitPoint;
    private Long pagoId;
}
