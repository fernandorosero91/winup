package com.winup.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winup.exception.ValidationException;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MercadoPagoSignatureValidatorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_accept_a_valid_signature() throws Exception {
        String payload = "{\"data\":{\"id\":\"12345\"}}";
        String signature = signature("12345", "req-1", "1700000000", "secret-test");
        MercadoPagoSignatureValidator validator = new MercadoPagoSignatureValidator(
                objectMapper,
                "secret-test"
        );

        assertThatCode(() -> validator.validate(payload, signature, "req-1"))
                .doesNotThrowAnyException();
    }

    @Test
    void should_reject_a_tampered_signature() {
        MercadoPagoSignatureValidator validator = new MercadoPagoSignatureValidator(
                objectMapper,
                "secret-test"
        );

        assertThatThrownBy(() -> validator.validate(
                "{\"data\":{\"id\":\"12345\"}}",
                "ts=1700000000,v1=deadbeef",
                "req-1"
        )).isInstanceOf(ValidationException.class);
    }

    @Test
    void should_skip_validation_when_the_secret_is_not_configured() {
        MercadoPagoSignatureValidator validator = new MercadoPagoSignatureValidator(objectMapper, "");

        assertThatCode(() -> validator.validate("{}", null, null))
                .doesNotThrowAnyException();
    }

    private String signature(String id, String requestId, String timestamp, String secret)
            throws Exception {
        String manifest = "id:" + id + ";request-id:" + requestId + ";ts:" + timestamp + ";";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "ts=" + timestamp + ",v1="
                + HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
    }
}
