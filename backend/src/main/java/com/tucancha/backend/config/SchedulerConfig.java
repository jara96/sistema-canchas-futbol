package com.tucancha.backend.config;

import com.tucancha.backend.service.ReservaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Tareas programadas del sistema.
 * - Cada 60s expira reservas PENDIENTE que no completaron el pago a tiempo,
 *   liberando el turno para que otro usuario pueda reservarlo.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SchedulerConfig {

    private final ReservaService reservaService;

    @Scheduled(fixedDelay = 60_000L, initialDelay = 30_000L)
    public void expirarReservasPendientes() {
        try {
            int n = reservaService.expirarPendientes();
            if (n > 0) log.info("Expiradas {} reservas PENDIENTE sin pago", n);
        } catch (Exception e) {
            log.error("Error expirando reservas pendientes", e);
        }
    }
}
