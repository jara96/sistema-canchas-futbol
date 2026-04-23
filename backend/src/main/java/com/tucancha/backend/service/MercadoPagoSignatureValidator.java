package com.tucancha.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Valida la firma HMAC-SHA256 del webhook de MercadoPago.
 * Doc: https://www.mercadopago.com.ar/developers/es/docs/your-integrations/notifications/webhooks
 *
 * Header x-signature: "ts=1700000000,v1=hexsha256..."
 * Manifest a firmar: "id:<dataId>;request-id:<xRequestId>;ts:<ts>;"
 */
@Component
@Slf4j
public class MercadoPagoSignatureValidator {

    @Value("${app.mercadopago.webhook-secret}")
    private String secret;

    @Value("${app.mercadopago.webhook-validate}")
    private boolean enabled;

    public boolean validar(String xSignature, String xRequestId, String dataId) {
        if (!enabled) return true;
        if (xSignature == null || dataId == null) {
            log.warn("Webhook MP sin x-signature o sin dataId");
            return false;
        }

        String ts = null;
        String v1 = null;
        for (String part : xSignature.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) continue;
            switch (kv[0].trim()) {
                case "ts" -> ts = kv[1].trim();
                case "v1" -> v1 = kv[1].trim();
            }
        }
        if (ts == null || v1 == null) {
            log.warn("Webhook MP x-signature sin ts/v1: {}", xSignature);
            return false;
        }

        String manifest = "id:" + dataId + ";request-id:"
                + (xRequestId == null ? "" : xRequestId) + ";ts:" + ts + ";";

        String calculado = hmacSha256Hex(secret, manifest);
        boolean ok = constantTimeEquals(calculado, v1);
        if (!ok) log.warn("Firma MP inválida. Esperada={} Recibida={}", calculado, v1);
        return ok;
    }

    private String hmacSha256Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Error calculando HMAC", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) diff |= a.charAt(i) ^ b.charAt(i);
        return diff == 0;
    }
}
