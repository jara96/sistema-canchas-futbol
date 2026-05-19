package com.tucancha.backend.controller;

import com.tucancha.backend.dto.PreferenciaResponse;
import com.tucancha.backend.dto.ReservaResponse;
import com.tucancha.backend.entity.Pago;
import com.tucancha.backend.entity.Reserva;
import com.tucancha.backend.security.CurrentUser;
import com.tucancha.backend.service.MercadoPagoService;
import com.tucancha.backend.service.MercadoPagoSignatureValidator;
import com.tucancha.backend.service.PagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@Slf4j
public class PagoController {

    private final MercadoPagoService mercadoPagoService;
    private final PagoService pagoService;
    private final MercadoPagoSignatureValidator signatureValidator;

    @PostMapping("/reservas/{reservaId}/preferencia")
    public PreferenciaResponse crearPreferencia(@PathVariable Long reservaId) {
        return mercadoPagoService.crearPreferencia(reservaId, CurrentUser.id());
    }

    /** Crea la preferencia MP para pagar la seña total de un torneo. */
    @PostMapping("/torneos/{torneoId}/preferencia")
    public PreferenciaResponse crearPreferenciaTorneo(@PathVariable Long torneoId) {
        return mercadoPagoService.crearPreferenciaTorneo(torneoId, CurrentUser.id());
    }

    /** Admin: genera preferencia MP para cobrar el saldo restante (presencial / QR). */
    @PostMapping("/reservas/{reservaId}/saldo")
    @PreAuthorize("hasRole('ADMIN')")
    public PreferenciaResponse crearPreferenciaSaldo(@PathVariable Long reservaId) {
        return mercadoPagoService.crearPreferenciaSaldo(reservaId);
    }

    /** Admin: busca una reserva por su código de retiro de 6 dígitos. */
    @GetMapping("/codigo/{codigo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservaResponse> buscarPorCodigo(@PathVariable String codigo) {
        Reserva r = mercadoPagoService.buscarPorCodigo(codigo);
        if (r == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ReservaResponse.from(r));
    }

    @GetMapping("/reservas/{reservaId}")
    public List<Pago> listarPorReserva(@PathVariable Long reservaId) {
        return pagoService.listarPorReserva(reservaId);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String type,
            @RequestParam(name = "id", required = false) String id,
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestBody(required = false) Map<String, Object> body) {

        log.info("Webhook MP: topic={} type={} id={} dataId={}", topic, type, id, dataId);

        String resolvedTopic = topic != null ? topic : type;
        String resolvedId = id != null ? id : dataId;

        if (resolvedId == null && body != null) {
            Object data = body.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                Object dId = dataMap.get("id");
                if (dId != null) resolvedId = String.valueOf(dId);
            }
            if (resolvedTopic == null) {
                Object t = body.get("type");
                if (t != null) resolvedTopic = String.valueOf(t);
            }
        }

        if (!signatureValidator.validar(xSignature, xRequestId, resolvedId)) {
            return ResponseEntity.status(401).build();
        }

        mercadoPagoService.procesarWebhook(resolvedTopic, resolvedId);
        return ResponseEntity.ok().build();
    }
}
