package com.winup.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winup.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class MercadoPagoSignatureValidator {
    private final ObjectMapper objectMapper;
    private final String secret;

    public MercadoPagoSignatureValidator(
            ObjectMapper objectMapper,
            @Value("${winup.payments.mercadopago.webhook-secret:}") String secret
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret;
    }

    public void validate(String payload, String signature, String requestId) {
        if (secret == null || secret.isBlank()) {
            return;
        }
        if (signature == null || signature.isBlank()) {
            throw new ValidationException("INVALID_SIGNATURE", "Falta la firma del webhook");
        }
        String timestamp = null;
        String hash = null;
        for (String part : signature.split(",")) {
            String[] pair = part.split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            if ("ts".equals(pair[0].trim())) {
                timestamp = pair[1].trim();
            }
            if ("v1".equals(pair[0].trim())) {
                hash = pair[1].trim();
            }
        }
        if (timestamp == null || hash == null || requestId == null) {
            throw new ValidationException("INVALID_SIGNATURE", "Firma incompleta");
        }
        String paymentId = paymentId(payload);
        String manifest = "id:" + paymentId + ";request-id:" + requestId + ";ts:" + timestamp + ";";
        String expected = hmac(manifest);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                hash.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new ValidationException("INVALID_SIGNATURE", "Firma inválida");
        }
    }

    private String paymentId(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String id = root.path("data").path("id").asText();
            if (id.isBlank()) {
                throw new ValidationException("INVALID_WEBHOOK", "Notificación sin pago");
            }
            return id;
        } catch (ValidationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ValidationException("INVALID_WEBHOOK", "Notificación inválida");
        }
    }

    private String hmac(String manifest) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new ValidationException("INVALID_SIGNATURE", "No se pudo verificar la firma");
        }
    }
}
