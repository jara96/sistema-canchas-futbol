package com.tucancha.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {
    // totales generales
    private long totalReservas;
    private long reservasConfirmadas;
    private long reservasPendientes;
    private long reservasCanceladas;
    private BigDecimal ingresosTotales;      // suma de pagos APROBADOS
    private BigDecimal ingresosSenia;
    private BigDecimal ingresosSaldo;
    private long senasPagadas;
    private long saldosPagados;
    private long saldosPendientes;           // reservas confirmadas sin saldo pagado

    private Map<String, Long> reservasPorEstado;           // estado -> cantidad
    private List<ItemCount> reservasPorCancha;             // cancha -> cantidad
    private List<IngresoPorCancha> ingresosPorCancha;      // cancha -> $
    private List<ItemCount> reservasPorDia;                // yyyy-MM-dd -> cantidad (últimos 30)
    private List<ItemCount> turnosPopulares;               // hora -> cantidad

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ItemCount {
        private String label;
        private long value;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IngresoPorCancha {
        private String cancha;
        private BigDecimal monto;
    }
}
