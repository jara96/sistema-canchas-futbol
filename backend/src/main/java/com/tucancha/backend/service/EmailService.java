package com.tucancha.backend.service;

import com.tucancha.backend.entity.Reserva;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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
}
