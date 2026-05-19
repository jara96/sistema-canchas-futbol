package com.tucancha.backend.service;

import com.tucancha.backend.entity.Reserva;
import com.tucancha.backend.entity.Torneo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Envío de notificaciones por email. Si no hay JavaMailSender configurado
 * (faltan las credenciales SMTP en application.properties), este servicio se
 * degrada silenciosamente: loguea el mensaje pero no falla la operación.
 */
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@tucancha.local}")
    private String fromAddress;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarCodigoRetiro(Reserva reserva) {
        String email = reserva.getUsuario().getEmail();
        String codigo = reserva.getCodigoRetiro();
        BigDecimal saldo = reserva.getTotal().subtract(reserva.getSenia());

        String asunto = "Tu código de reserva " + reserva.getCancha().getNombre();
        String cuerpo = """
                ¡Hola %s!

                Confirmamos el pago de la seña de tu reserva en %s para el día %s
                en el turno %s a %s.

                Tu código de retiro es: %s

                Cuando llegues a la cancha, mostrá este código al encargado para
                completar el pago del saldo restante: $%s.

                Total: $%s
                Seña pagada: $%s
                Saldo a pagar en la cancha: $%s

                ¡Te esperamos!
                """.formatted(
                reserva.getUsuario().getNombre(),
                reserva.getCancha().getNombre(),
                reserva.getFecha(),
                reserva.getTurno().getHoraInicio(),
                reserva.getTurno().getHoraFin(),
                codigo,
                saldo,
                reserva.getTotal(),
                reserva.getSenia(),
                saldo
        );

        if (!enabled || mailSender == null) {
            log.info("[Email deshabilitado] Para {} asunto='{}' codigo={} saldo={}",
                    email, asunto, codigo, saldo);
            return;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(email);
            msg.setSubject(asunto);
            msg.setText(cuerpo);
            mailSender.send(msg);
            log.info("Email de código enviado a {}", email);
        } catch (Exception ex) {
            log.warn("Fallo enviando email a {}: {}", email, ex.getMessage(), ex);
        }
    }

    /**
     * Envía un email único con la lista completa de turnos y códigos del torneo.
     */
    public void enviarCodigosTorneo(Torneo torneo, List<Reserva> reservas) {
        String email = torneo.getUsuario().getEmail();
        BigDecimal totalSenia = torneo.getTotalSenia();
        BigDecimal total = torneo.getTotal();
        BigDecimal saldoTotal = total.subtract(totalSenia);

        StringBuilder lista = new StringBuilder();
        for (Reserva r : reservas) {
            BigDecimal saldoTurno = r.getTotal().subtract(r.getSenia());
            lista.append("📅 ")
                    .append(r.getFecha()).append(" — ")
                    .append(r.getTurno().getHoraInicio()).append(" a ").append(r.getTurno().getHoraFin())
                    .append(" — ").append(r.getCancha().getNombre())
                    .append(" — Código: ").append(r.getCodigoRetiro())
                    .append(" — Saldo: $").append(saldoTurno)
                    .append("\n");
        }

        String asunto = "🏆 Tu torneo fue confirmado - " + reservas.size() + " turnos";
        String cuerpo = """
                ¡Hola %s!

                Tu torneo en %s fue confirmado. Acá tenés la lista completa
                de turnos reservados con sus códigos individuales.

                Turnos reservados: %d
                Total: $%s
                Seña pagada: $%s
                Saldo total a pagar en la cancha: $%s

                === LISTA DE TURNOS ===
                %s
                Mostrá cada código al llegar a la cancha para que cobren el saldo
                de ese turno.

                Si no te presentás a algún turno, perdés la seña de ese turno
                (los demás siguen vigentes).

                ¡Te esperamos!
                """.formatted(
                torneo.getUsuario().getNombre(),
                torneo.getCancha().getNombre(),
                reservas.size(),
                total,
                totalSenia,
                saldoTotal,
                lista.toString()
        );

        if (!enabled || mailSender == null) {
            log.info("[Email deshabilitado] Torneo {} -> {} con {} códigos",
                    torneo.getId(), email, reservas.size());
            return;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(email);
            msg.setSubject(asunto);
            msg.setText(cuerpo);
            mailSender.send(msg);
            log.info("Email de torneo {} enviado a {}", torneo.getId(), email);
        } catch (Exception ex) {
            log.warn("Fallo enviando email de torneo a {}: {}", email, ex.getMessage(), ex);
        }
    }
}
