package com.tucancha.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class MercadoPagoSignatureValidatorTest {

    private MercadoPagoSignatureValidator validator;
    private final String secret = "mi-secreto";

    @BeforeEach
    void setUp() {
        validator = new MercadoPagoSignatureValidator();
        ReflectionTestUtils.setField(validator, "secret", secret);
        ReflectionTestUtils.setField(validator, "enabled", true);
    }

    @Test
    void validar_aceptaFirmaCorrecta() throws Exception {
        String dataId = "12345";
        String requestId = "req-1";
        String ts = "1700000000";
        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String v1 = HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));

        String header = "ts=" + ts + ",v1=" + v1;
        assertThat(validator.validar(header, requestId, dataId)).isTrue();
    }

    @Test
    void validar_rechazaFirmaInvalida() {
        String header = "ts=1700000000,v1=ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
        assertThat(validator.validar(header, "req", "12345")).isFalse();
    }

    @Test
    void validar_disabled_devuelveTrue() {
        ReflectionTestUtils.setField(validator, "enabled", false);
        assertThat(validator.validar(null, null, null)).isTrue();
    }
}
