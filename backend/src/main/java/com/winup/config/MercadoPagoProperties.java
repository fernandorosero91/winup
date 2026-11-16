package com.winup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "winup.payments.mercadopago")
public record MercadoPagoProperties(
        String accessToken,
        String publicKey,
        boolean sandbox,
        String notificationUrl,
        String webhookSecret,
        String backUrl
) {
    public MercadoPagoProperties {
        accessToken = accessToken == null ? "" : accessToken;
        publicKey = publicKey == null ? "" : publicKey;
        notificationUrl = notificationUrl == null || notificationUrl.isBlank()
                ? "http://localhost:8080/api/v1/webhooks/mercadopago"
                : notificationUrl;
        webhookSecret = webhookSecret == null ? "" : webhookSecret;
        backUrl = backUrl == null || backUrl.isBlank()
                ? "http://localhost:4200/cartera"
                : backUrl;
    }
}
