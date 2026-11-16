package com.winup.service.payment;

import com.winup.config.MercadoPagoProperties;
import com.winup.exception.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "winup.payments.payout-provider", havingValue = "mercadopago")
public class MercadoPagoPayoutGateway implements PayoutGateway {
    private final RestTemplate restTemplate;
    private final MercadoPagoProperties properties;

    @Autowired
    public MercadoPagoPayoutGateway(RestTemplateBuilder builder, MercadoPagoProperties properties) {
        this(
                builder
                        .rootUri("https://api.mercadopago.com")
                        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken())
                        .build(),
                properties
        );
    }

    MercadoPagoPayoutGateway(RestTemplate restTemplate, MercadoPagoProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public String provider() {
        return "MERCADOPAGO";
    }

    @Override
    public PayoutResult payout(PayoutOrder order) {
        if (order.destination() == null || order.destination().isBlank()) {
            throw new ValidationException(
                    "PAYOUT_DESTINATION_REQUIRED",
                    "El retiro requiere una cuenta destino de Mercado Pago"
            );
        }
        Map<String, Object> transaction = Map.of(
                "external_reference", order.withdrawalId().toString(),
                "type", "account",
                "account", Map.of("email", order.destination()),
                "amount", Map.of("currency", order.currency(), "value", order.amountCop()),
                "description", "Retiro WinUp"
        );
        Map<String, Object> body = Map.of(
                "external_reference", order.withdrawalId().toString(),
                "description", "Retiro WinUp",
                "transactions", List.of(transaction)
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Idempotency-Key", order.withdrawalId().toString());

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/v1/payouts",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            Map<?, ?> payload = response.getBody();
            String reference = payload == null ? null : String.valueOf(payload.get("id"));
            if (reference == null || "null".equals(reference)) {
                return new PayoutResult(null, PayoutState.FAILED);
            }
            return new PayoutResult(reference, PayoutState.PENDING);
        } catch (RestClientException exception) {
            return new PayoutResult(null, PayoutState.FAILED);
        }
    }
}
