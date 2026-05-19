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
import com.tucancha.backend.entity.Torneo;
import com.tucancha.backend.enums.EstadoPago;
import com.tucancha.backend.enums.EstadoReserva;
import com.tucancha.backend.enums.EstadoTorneo;
import com.tucancha.backend.exception.BadRequestException;
import com.tucancha.backend.exception.ResourceNotFoundException;
import com.tucancha.backend.repository.PagoRepository;
import com.tucancha.backend.repository.ReservaRepository;
import com.tucancha.backend.repository.TorneoRepository;
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
    /** Prefijo que usamos en external_reference para identificar el pago de la seña de un torneo. */
    private static final String PREFIJO_TORNEO = "TORNEO-";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final TorneoRepository torneoRepository;
    private final EmailService emailService;

    @Value("${app.mercadopago.webhook-url}")
    private String webhookUrl;

    @Value("${app.oauth2.redirect-uri}")
    private String frontendBase;

    /** URL pública (HTTPS) del sitio para back_urls de MP. Si está vacía,
     *  derivamos del frontendBase, pero MP no acepta localhost con auto_return. */
    @Value("${app.public.url:}")
    private String publicUrl;

    /** Devuelve la base pública del sitio, priorizando app.public.url si existe. */
    private String resolverSiteBase() {
        if (publicUrl != null && !publicUrl.isBlank()) {
            return publicUrl.replaceAll("/+$", "");
        }
        String base = frontendBase == null ? "" : frontendBase.replaceAll("/oauth2/?.*$", "");
        if (base.isBlank()) base = "http://localhost";
        return base;
    }

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
        if (reserva.getFecha() != null && reserva.getFecha().isBefore(java.time.LocalDate.now())) {
            throw new BadRequestException("No se puede cobrar el saldo: el día del turno ya pasó.");
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
     * Crea una preferencia de pago en MercadoPago para la SEÑA total de un torneo.
     * El external_reference se prefija con TORNEO- para que el webhook procese
     * todas las reservas asociadas como un grupo.
     */
    public PreferenciaResponse crearPreferenciaTorneo(Long torneoId, Long usuarioIdSolicitante) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new ResourceNotFoundException("Torneo no encontrado"));
        if (!torneo.getUsuario().getId().equals(usuarioIdSolicitante)) {
            throw new BadRequestException("No podés pagar un torneo ajeno");
        }
        if (torneo.getEstado() != EstadoTorneo.PENDIENTE) {
            throw new BadRequestException("El torneo no está pendiente de pago");
        }
        List<Reserva> reservas = reservaRepository.findByTorneoId(torneo.getId());
        if (reservas.isEmpty()) {
            throw new BadRequestException("El torneo no tiene reservas asociadas");
        }

        // Anclamos el Pago a la primera reserva del torneo (Pago.reserva es NOT NULL).
        Reserva anchor = reservas.get(0);

        try {
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .id("TORNEO-" + torneo.getId())
                    .title("Seña torneo " + torneo.getCancha().getNombre()
                            + " - " + reservas.size() + " turnos")
                    .description("Modalidad " + torneo.getModalidad())
                    .quantity(1)
                    .currencyId("ARS")
                    .unitPrice(torneo.getTotalSenia())
                    .build();

            String siteBase = resolverSiteBase();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(siteBase + "/mis-reservas?pago=success&torneo=" + torneo.getId())
                    .pending(siteBase + "/mis-reservas?pago=pending&torneo=" + torneo.getId())
                    .failure(siteBase + "/mis-reservas?pago=failure&torneo=" + torneo.getId())
                    .build();

            PreferenceRequest req = PreferenceRequest.builder()
                    .items(List.of(item))
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .externalReference(PREFIJO_TORNEO + torneo.getId())
                    .notificationUrl(webhookUrl)
                    .build();

            Preference preference = new PreferenceClient().create(req);

            Pago pago = Pago.builder()
                    .reserva(anchor)
                    .monto(torneo.getTotalSenia())
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
            log.error("Error MP torneo: status={} body={}", e.getStatusCode(), e.getApiResponse().getContent());
            throw new BadRequestException("Error MercadoPago: " + e.getMessage());
        } catch (MPException e) {
            throw new BadRequestException("Error MercadoPago: " + e.getMessage());
        }
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

            String siteBase = resolverSiteBase();

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

            boolean esTorneo = externalRef != null && externalRef.startsWith(PREFIJO_TORNEO);
            boolean esSaldo = !esTorneo && externalRef != null && externalRef.startsWith(PREFIJO_SALDO);

            EstadoPago nuevoEstado = mapearEstado(status);

            // ===== TORNEO =====
            if (esTorneo) {
                Long torneoId = Long.parseLong(externalRef.substring(PREFIJO_TORNEO.length()));
                Torneo torneo = torneoRepository.findById(torneoId)
                        .orElseThrow(() -> new ResourceNotFoundException("Torneo no encontrado: " + torneoId));
                List<Reserva> reservas = reservaRepository.findByTorneoId(torneoId);

                final Long anchorId = reservas.isEmpty() ? null : reservas.get(0).getId();
                Pago pagoT = pagoRepository.findByMpPaymentId(String.valueOf(mpPayment.getId()))
                        .orElseGet(() -> {
                            Reserva anchor = reservaRepository.findById(anchorId)
                                    .orElseThrow(() -> new ResourceNotFoundException("Reserva anchor no encontrada"));
                            return Pago.builder()
                                    .reserva(anchor)
                                    .monto(torneo.getTotalSenia())
                                    .estado(EstadoPago.PENDIENTE)
                                    .build();
                        });
                pagoT.setMpPaymentId(String.valueOf(mpPayment.getId()));
                pagoT.setEstado(nuevoEstado);
                pagoT.setMetodoPago(metodo);
                pagoRepository.save(pagoT);

                if (nuevoEstado == EstadoPago.APROBADO && torneo.getEstado() != EstadoTorneo.CONFIRMADO) {
                    for (Reserva r : reservas) {
                        r.setEstado(EstadoReserva.CONFIRMADA);
                        if (r.getCodigoRetiro() == null || r.getCodigoRetiro().isBlank()) {
                            r.setCodigoRetiro(generarCodigoUnico());
                        }
                    }
                    reservaRepository.saveAll(reservas);
                    torneo.setEstado(EstadoTorneo.CONFIRMADO);
                    torneoRepository.save(torneo);
                    try {
                        emailService.enviarCodigosTorneo(torneo, reservas);
                    } catch (Exception ex) {
                        log.warn("No se pudo enviar email de torneo {}: {}", torneoId, ex.getMessage());
                    }
                    log.info("Torneo {} confirmado con {} reservas", torneoId, reservas.size());
                }
                return;
            }

            // ===== RESERVA INDIVIDUAL (seña o saldo) =====
            String reservaIdStr = esSaldo ? externalRef.substring(PREFIJO_SALDO.length()) : externalRef;
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
