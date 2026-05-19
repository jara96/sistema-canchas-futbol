package com.tucancha.backend.config;

import com.tucancha.backend.service.ReservaService;
import com.tucancha.backend.service.TorneoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Tareas programadas del sistema.
 * - Cada 60s expira reservas PENDIENTE (individuales o de torneo) que no
 *   completaron el pago a tiempo, liberando los turnos.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SchedulerConfig {

    private final ReservaService reservaService;
    private final TorneoService torneoService;

    @Scheduled(fixedDelay = 60_000L, initialDelay = 30_000L)
    public void expirarReservasPendientes() {
        try {
            int n = reservaService.expirarPendientes();
            if (n > 0) log.info("Expiradas {} reservas PENDIENTE sin pago", n);
        } catch (Exception e) {
            log.error("Error expirando reservas pendientes", e);
        }
        try {
            int t = torneoService.expirarPendientes();
            if (t > 0) log.info("Expirados {} torneos PENDIENTE sin pago", t);
        } catch (Exception e) {
            log.error("Error expirando torneos pendientes", e);
        }
    }

    /**
     * Cada hora: marca como FINALIZADA las reservas CONFIRMADAS cuya fecha de juego
     * ya pasó (al cruzar 00:00 del día siguiente), y como CANCELADA las PENDIENTES
     * pasadas (nunca se concretaron).
     */
    @Scheduled(fixedDelay = 3_600_000L, initialDelay = 60_000L)
    public void finalizarPasadas() {
        try {
            int n = reservaService.finalizarPasadas();
            if (n > 0) log.info("Finalizadas/canceladas automáticamente {} reservas pasadas", n);
        } catch (Exception e) {
            log.error("Error finalizando reservas pasadas", e);
        }
    }
}
