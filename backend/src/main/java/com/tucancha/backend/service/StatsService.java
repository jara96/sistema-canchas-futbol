package com.tucancha.backend.service;

import com.tucancha.backend.dto.StatsResponse;
import com.tucancha.backend.entity.Pago;
import com.tucancha.backend.entity.Reserva;
import com.tucancha.backend.enums.EstadoPago;
import com.tucancha.backend.enums.EstadoReserva;
import com.tucancha.backend.repository.PagoRepository;
import com.tucancha.backend.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;

    public StatsResponse dashboard() {
        List<Reserva> reservas = reservaRepository.findAll();
        List<Pago> pagos = pagoRepository.findAll();

        // ===== Conteos por estado =====
        Map<String, Long> porEstado = reservas.stream().collect(
                Collectors.groupingBy(r -> r.getEstado().name(), Collectors.counting()));

        long confirmadas = porEstado.getOrDefault("CONFIRMADA", 0L);
        long pendientes = porEstado.getOrDefault("PENDIENTE", 0L);
        long canceladas = porEstado.getOrDefault("CANCELADA", 0L);

        // ===== Ingresos =====
        List<Pago> aprobados = pagos.stream()
                .filter(p -> p.getEstado() == EstadoPago.APROBADO)
                .toList();

        // Heurística: si el monto del pago es igual a la seña, es seña; si es igual a total-seña, es saldo.
        BigDecimal ingresosSenia = BigDecimal.ZERO;
        BigDecimal ingresosSaldo = BigDecimal.ZERO;
        long senasCount = 0;
        long saldosCount = 0;
        for (Pago p : aprobados) {
            Reserva r = p.getReserva();
            BigDecimal monto = p.getMonto() != null ? p.getMonto() : BigDecimal.ZERO;
            BigDecimal saldoEsperado = r.getTotal().subtract(r.getSenia());
            if (p.getMonto() != null && p.getMonto().compareTo(saldoEsperado) == 0
                    && r.getSenia().compareTo(saldoEsperado) != 0) {
                ingresosSaldo = ingresosSaldo.add(monto);
                saldosCount++;
            } else {
                ingresosSenia = ingresosSenia.add(monto);
                senasCount++;
            }
        }
        BigDecimal ingresosTotales = ingresosSenia.add(ingresosSaldo);

        long saldosPendientes = reservas.stream()
                .filter(r -> r.getEstado() == EstadoReserva.CONFIRMADA
                        && !Boolean.TRUE.equals(r.getSaldoPagado()))
                .count();

        // ===== Reservas por cancha =====
        Map<String, Long> porCanchaMap = reservas.stream()
                .filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
                .collect(Collectors.groupingBy(
                        r -> r.getCancha().getNombre(),
                        Collectors.counting()));
        List<StatsResponse.ItemCount> porCancha = porCanchaMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> StatsResponse.ItemCount.builder()
                        .label(e.getKey()).value(e.getValue()).build())
                .toList();

        // ===== Ingresos por cancha (solo APROBADOS) =====
        Map<String, BigDecimal> ingresosCanchaMap = new HashMap<>();
        for (Pago p : aprobados) {
            String nombre = p.getReserva().getCancha().getNombre();
            ingresosCanchaMap.merge(nombre, p.getMonto() != null ? p.getMonto() : BigDecimal.ZERO, BigDecimal::add);
        }
        List<StatsResponse.IngresoPorCancha> ingresosPorCancha = ingresosCanchaMap.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> StatsResponse.IngresoPorCancha.builder()
                        .cancha(e.getKey()).monto(e.getValue()).build())
                .toList();

        // ===== Reservas por día (últimos 30 días) =====
        LocalDate hoy = LocalDate.now();
        LocalDate desde = hoy.minusDays(29);
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

        // Inicializar todos los días con 0
        Map<String, Long> porDia = new LinkedHashMap<>();
        for (int i = 0; i < 30; i++) {
            porDia.put(desde.plusDays(i).format(fmt), 0L);
        }
        for (Reserva r : reservas) {
            if (r.getEstado() == EstadoReserva.CANCELADA) continue;
            LocalDate f = r.getFecha();
            if (f == null || f.isBefore(desde) || f.isAfter(hoy)) continue;
            String key = f.format(fmt);
            porDia.merge(key, 1L, Long::sum);
        }
        List<StatsResponse.ItemCount> reservasPorDia = porDia.entrySet().stream()
                .map(e -> StatsResponse.ItemCount.builder()
                        .label(e.getKey()).value(e.getValue()).build())
                .toList();

        // ===== Turnos más populares (por hora de inicio) =====
        Map<String, Long> porTurnoMap = reservas.stream()
                .filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
                .collect(Collectors.groupingBy(
                        r -> r.getTurno().getHoraInicio().toString().substring(0, 5),
                        Collectors.counting()));
        List<StatsResponse.ItemCount> turnosPopulares = porTurnoMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> StatsResponse.ItemCount.builder()
                        .label(e.getKey()).value(e.getValue()).build())
                .toList();

        return StatsResponse.builder()
                .totalReservas(reservas.size())
                .reservasConfirmadas(confirmadas)
                .reservasPendientes(pendientes)
                .reservasCanceladas(canceladas)
                .ingresosTotales(ingresosTotales)
                .ingresosSenia(ingresosSenia)
                .ingresosSaldo(ingresosSaldo)
                .senasPagadas(senasCount)
                .saldosPagados(saldosCount)
                .saldosPendientes(saldosPendientes)
                .reservasPorEstado(porEstado)
                .reservasPorCancha(porCancha)
                .ingresosPorCancha(ingresosPorCancha)
                .reservasPorDia(reservasPorDia)
                .turnosPopulares(turnosPopulares)
                .build();
    }
}
