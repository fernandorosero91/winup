package com.winup.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winup.config.MercadoPagoProperties;
import com.winup.exception.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "winup.payments.provider", havingValue = "mercadopago")
public class MercadoPagoGateway implements PaymentGateway {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MercadoPagoProperties properties;
    private final ReturnUrlPolicy returnUrlPolicy;

    @Autowired
    public MercadoPagoGateway(
            RestTemplateBuilder builder,
            ObjectMapper objectMapper,
            MercadoPagoProperties properties,
            ReturnUrlPolicy returnUrlPolicy
    ) {
        this(
                builder
                        .rootUri("https://api.mercadopago.com")
                        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken())
                        .build(),
                objectMapper,
                properties,
                returnUrlPolicy
        );
    }

    MercadoPagoGateway(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            MercadoPagoProperties properties,
            ReturnUrlPolicy returnUrlPolicy
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.returnUrlPolicy = returnUrlPolicy;
    }

    @Override
    public String provider() {
        return "MERCADOPAGO";
    }

    @Override
    public CheckoutSession createDeposit(DepositOrder order) {
        String returnUrl = returnUrlPolicy.publicReturnUrl(order.returnUrl());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", List.of(Map.of(
                "title", order.description(),
                "quantity", 1,
                "currency_id", order.currency(),
                "unit_price", order.amountCop()
        )));
        body.put("external_reference", order.intentId().toString());
        body.put("notification_url", properties.notificationUrl());
        body.put("back_urls", Map.of(
                "success", returnUrl,
                "failure", returnUrl,
                "pending", returnUrl
        ));
        if (returnUrl.startsWith("https://")) {
            body.put("auto_return", "approved");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject("/checkout/preferences", body, Map.class);
        if (response == null) {
            throw new ValidationException("PAYMENT_ERROR", "No se pudo crear el checkout");
        }
        String reference = String.valueOf(response.get("id"));
        String checkout = properties.sandbox()
                ? (String) response.get("sandbox_init_point")
                : (String) response.get("init_point");
        if (checkout == null) {
            checkout = (String) response.get("init_point");
        }
        return new CheckoutSession(reference, checkout);
    }

    @Override
    public ProviderPayment fetchPayment(String providerReference) {
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(
                "/v1/payments/{id}",
                Map.class,
                providerReference
        );
        if (response == null) {
            throw new ValidationException("PAYMENT_NOT_FOUND", "Pago no encontrado");
        }
        return new ProviderPayment(
                String.valueOf(response.get("id")),
                String.valueOf(response.get("external_reference")),
                new BigDecimal(String.valueOf(response.get("transaction_amount"))),
                String.valueOf(response.get("currency_id")),
                String.valueOf(response.get("status"))
        );
    }

    @Override
    public ProviderNotification parseNotification(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String paymentReference = root.path("data").path("id").asText();
            String eventId = root.path("id").asText();
            if (paymentReference.isBlank()) {
                throw new ValidationException("INVALID_WEBHOOK", "Notificación sin pago");
            }
            return new ProviderNotification(eventId.isBlank() ? paymentReference : eventId, paymentReference);
        } catch (Exception exception) {
            throw new ValidationException("INVALID_WEBHOOK", "Notificación inválida");
        }
    }
}
