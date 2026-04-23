package com.tucancha.backend.service;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.*;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.tucancha.backend.dto.PreferenciaResponse;
import com.tucancha.backend.entity.Pago;
import com.tucancha.backend.entity.Reserva;
import com.tucancha.backend.enums.EstadoPago;
import com.tucancha.backend.enums.EstadoReserva;
import com.tucancha.backend.exception.BadRequestException;
import com.tucancha.backend.exception.ResourceNotFoundException;
import com.tucancha.backend.repository.PagoRepository;
import com.tucancha.backend.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MercadoPagoService {

    /** Prefijo que usamos en external_reference cuando la preferencia corresponde
     *  al saldo restante (no a la seña). */
    private static final String PREFIJO_SALDO = "SALDO-";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final EmailService emailService;

    @Value("${app.mercadopago.webhook-url}")
    private String webhookUrl;

    @Value("${app.oauth2.redirect-uri}")
    private String frontendBase;

    /**
     * Crea una preferencia de pago en MercadoPago para la seña de la reserva
     * y registra un Pago en estado PENDIENTE.
     */
    public PreferenciaResponse crearPreferencia(Long reservaId, Long usuarioIdSolicitante) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));

        if (!reserva.getUsuario().getId().equals(usuarioIdSolicitante)) {
            throw new BadRequestException("No podés pagar una reserva ajena");
        }
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new BadRequestException("La reserva está cancelada");
        }

        return crearPreferenciaInterno(reserva, reserva.getSenia(),
                "Seña reserva " + reserva.getCancha().getNombre() + " - " + reserva.getFecha(),
                String.valueOf(reserva.getId()));
    }

    /**
     * Crea una preferencia para cobrar el SALDO (total - seña) de una reserva CONFIRMADA.
     * Usado por el admin en la cancha para generar un QR presencial.
     */
    public PreferenciaResponse crearPreferenciaSaldo(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));

        if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new BadRequestException("La reserva no está confirmada");
        }
        if (Boolean.TRUE.equals(reserva.getSaldoPagado())) {
            throw new BadRequestException("El saldo de esta reserva ya fue pagado");
        }

        BigDecimal saldo = reserva.getTotal().subtract(reserva.getSenia());
        if (saldo.signum() <= 0) {
            throw new BadRequestException("No hay saldo pendiente de pago");
        }

        return crearPreferenciaInterno(reserva, saldo,
                "Saldo reserva " + reserva.getCancha().getNombre() + " - " + reserva.getFecha(),
                PREFIJO_SALDO + reserva.getId());
    }

    /**
     * Busca una reserva por su código de retiro (6 dígitos entregado al usuario).
     * Devuelve null si no existe o ya fue cobrada.
     */
    @Transactional(readOnly = true)
    public Reserva buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) return null;
        return reservaRepository.findByCodigoRetiro(codigo.trim()).orElse(null);
    }

    private PreferenciaResponse crearPreferenciaInterno(Reserva reserva,
                                                        BigDecimal monto,
                                                        String titulo,
                                                        String externalRef) {
        try {
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .id(String.valueOf(reserva.getId()))
                    .title(titulo)
                    .description("Turno " + reserva.getTurno().getHoraInicio()
                            + " a " + reserva.getTurno().getHoraFin())
                    .quantity(1)
                    .currencyId("ARS")
                    .unitPrice(monto)
                    .build();

            String siteBase = frontendBase.replaceAll("/oauth2/?.*$", "");
            if (siteBase.isBlank()) siteBase = "http://localhost";

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(siteBase + "/mis-reservas?pago=success&reserva=" + reserva.getId())
                    .pending(siteBase + "/mis-reservas?pago=pending&reserva=" + reserva.getId())
                    .failure(siteBase + "/mis-reservas?pago=failure&reserva=" + reserva.getId())
                    .build();

            PreferenceRequest req = PreferenceRequest.builder()
                    .items(List.of(item))
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .externalReference(externalRef)
                    .notificationUrl(webhookUrl)
                    .build();

            Preference preference = new PreferenceClient().create(req);

            Pago pago = Pago.builder()
                    .reserva(reserva)
                    .monto(monto)
                    .estado(EstadoPago.PENDIENTE)
                    .mpPreferenceId(preference.getId())
                    .build();
            pago = pagoRepository.save(pago);

            return PreferenciaResponse.builder()
                    .preferenceId(preference.getId())
                    .initPoint(preference.getInitPoint())
                    .sandboxInitPoint(preference.getSandboxInitPoint())
                    .pagoId(pago.getId())
                    .build();

        } catch (MPApiException e) {
            log.error("Error MP: status={} body={}", e.getStatusCode(), e.getApiResponse().getContent());
            throw new BadRequestException("Error MercadoPago: " + e.getMessage());
        } catch (MPException e) {
            throw new BadRequestException("Error MercadoPago: " + e.getMessage());
        }
    }

    /**
     * Procesa la notificación IPN/Webhook de MercadoPago.
     * MP nos manda el id de pago; consultamos la API y actualizamos.
     */
    public void procesarWebhook(String topic, String resourceId) {
        if (resourceId == null || resourceId.isBlank()) return;
        if (topic != null && !topic.equalsIgnoreCase("payment")) {
            log.info("Topic ignorado: {}", topic);
            return;
        }

        try {
            Payment mpPayment = new PaymentClient().get(Long.parseLong(resourceId));

            String externalRef = mpPayment.getExternalReference();
            String status = mpPayment.getStatus();
            String metodo = mpPayment.getPaymentMethodId();

            boolean esSaldo = externalRef != null && externalRef.startsWith(PREFIJO_SALDO);
            String reservaIdStr = esSaldo ? externalRef.substring(PREFIJO_SALDO.length()) : externalRef;

            EstadoPago nuevoEstado = mapearEstado(status);

            final String reservaIdFinal = reservaIdStr;
            Pago pago = pagoRepository.findByMpPaymentId(String.valueOf(mpPayment.getId()))
                    .orElseGet(() -> crearDesdeWebhook(mpPayment, reservaIdFinal));

            pago.setMpPaymentId(String.valueOf(mpPayment.getId()));
            pago.setEstado(nuevoEstado);
            pago.setMetodoPago(metodo);
            pagoRepository.save(pago);

            if (nuevoEstado == EstadoPago.APROBADO) {
                Reserva r = pago.getReserva();
                if (esSaldo) {
                    // El saldo se pagó en la cancha
                    r.setSaldoPagado(true);
                    reservaRepository.save(r);
                    log.info("Saldo aprobado para reserva {}", r.getId());
                } else {
                    // Es la seña: confirmamos la reserva y generamos código
                    r.setEstado(EstadoReserva.CONFIRMADA);
                    if (r.getCodigoRetiro() == null || r.getCodigoRetiro().isBlank()) {
                        r.setCodigoRetiro(generarCodigoUnico());
                    }
                    reservaRepository.save(r);

                    // Notificar al usuario por email (silencioso si falla)
                    try {
                        emailService.enviarCodigoRetiro(r);
                    } catch (Exception ex) {
                        log.warn("No se pudo enviar email de código a {}: {}",
                                r.getUsuario().getEmail(), ex.getMessage());
                    }
                }
            }

        } catch (MPException | MPApiException e) {
            log.error("Error procesando webhook MP", e);
            throw new BadRequestException("Error procesando webhook MercadoPago");
        }
    }

    private Pago crearDesdeWebhook(Payment mpPayment, String reservaIdStr) {
        Long reservaId = Long.parseLong(reservaIdStr);
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada: " + reservaId));
        return Pago.builder()
                .reserva(reserva)
                .monto(reserva.getSenia())
                .estado(EstadoPago.PENDIENTE)
                .build();
    }

    private EstadoPago mapearEstado(String mpStatus) {
        if (mpStatus == null) return EstadoPago.PENDIENTE;
        return switch (mpStatus) {
            case "approved" -> EstadoPago.APROBADO;
            case "rejected" -> EstadoPago.RECHAZADO;
            case "refunded", "charged_back" -> EstadoPago.REEMBOLSADO;
            case "cancelled" -> EstadoPago.CANCELADO;
            default -> EstadoPago.PENDIENTE;
        };
    }

    private String generarCodigoUnico() {
        for (int i = 0; i < 10; i++) {
            String code = String.format("%06d", RANDOM.nextInt(1_000_000));
            if (!reservaRepository.existsByCodigoRetiro(code)) return code;
        }
        // fallback con timestamp si hay colisiones improbables
        return String.format("%06d", (int) (System.currentTimeMillis() % 1_000_000));
    }
}
