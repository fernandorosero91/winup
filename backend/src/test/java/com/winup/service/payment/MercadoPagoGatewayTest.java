package com.winup.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winup.config.MercadoPagoProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MercadoPagoGatewayTest {
    private final RestTemplate restTemplate = new RestTemplate();
    private final MockRestServiceServer server;
    private final MercadoPagoGateway gateway = new MercadoPagoGateway(
            restTemplate,
            new ObjectMapper(),
            new MercadoPagoProperties(
                    "test-token",
                    "test-public-key",
                    true,
                    "https://api.winup.test/webhooks/mercadopago",
                    "",
                    "https://winup.test/cartera"
            ),
            new ReturnUrlPolicy(
                    "http://localhost:4200, http://127.0.0.1:8080, https://winup.test",
                    "https://winup.test/cartera",
                    "https://winup.test",
                    "https://winup.test/cartera"
            )
    );

    MercadoPagoGatewayTest() {
        restTemplate.setUriTemplateHandler(
                new org.springframework.web.util.DefaultUriBuilderFactory("https://api.mercadopago.com")
        );
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void should_create_a_sandbox_preference() {
        server.expect(requestTo("https://api.mercadopago.com/checkout/preferences"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"external_reference\"")))
                .andRespond(withSuccess("""
                        {
                          "id": "pref-1",
                          "init_point": "https://mercadopago.test/init",
                          "sandbox_init_point": "https://mercadopago.test/sandbox"
                        }
                        """, MediaType.APPLICATION_JSON));

        CheckoutSession session = gateway.createDeposit(
                new DepositOrder(UUID.randomUUID(), new BigDecimal("50000.00"), "COP", "Saldo WinUp")
        );

        assertThat(session.providerReference()).isEqualTo("pref-1");
        assertThat(session.checkoutUrl()).isEqualTo("https://mercadopago.test/sandbox");
        server.verify();
    }

    @Test
    void should_use_requested_return_url_for_back_urls() {
        server.expect(requestTo("https://api.mercadopago.com/checkout/preferences"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("https://winup.test/api/v1/payments/return/app")))
                .andExpect(content().string(containsString("\"auto_return\":\"approved\"")))
                .andRespond(withSuccess("""
                        {
                          "id": "pref-2",
                          "init_point": "https://mercadopago.test/init",
                          "sandbox_init_point": "https://mercadopago.test/sandbox"
                        }
                        """, MediaType.APPLICATION_JSON));

        gateway.createDeposit(new DepositOrder(
                UUID.randomUUID(),
                new BigDecimal("20000.00"),
                "COP",
                "Saldo WinUp",
                "http://127.0.0.1:8080/api/v1/payments/return"
        ));

        server.verify();
    }

    @Test
    void should_fall_back_when_return_url_is_not_allowed() {
        server.expect(requestTo("https://api.mercadopago.com/checkout/preferences"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"success\":\"https://winup.test/cartera\"")))
                .andExpect(content().string(not(containsString("sitio-desconocido"))))
                .andExpect(content().string(containsString("\"auto_return\":\"approved\"")))
                .andRespond(withSuccess("""
                        {
                          "id": "pref-3",
                          "init_point": "https://mercadopago.test/init",
                          "sandbox_init_point": "https://mercadopago.test/sandbox"
                        }
                        """, MediaType.APPLICATION_JSON));

        gateway.createDeposit(new DepositOrder(
                UUID.randomUUID(),
                new BigDecimal("20000.00"),
                "COP",
                "Saldo WinUp",
                "https://sitio-desconocido.test/cartera"
        ));

        server.verify();
    }

    @Test
    void should_keep_public_return_url() {
        server.expect(requestTo("https://api.mercadopago.com/checkout/preferences"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("https://winup.test/cartera")))
                .andExpect(content().string(not(containsString("/payments/return"))))
                .andRespond(withSuccess("""
                        {
                          "id": "pref-4",
                          "init_point": "https://mercadopago.test/init",
                          "sandbox_init_point": "https://mercadopago.test/sandbox"
                        }
                        """, MediaType.APPLICATION_JSON));

        gateway.createDeposit(new DepositOrder(
                UUID.randomUUID(),
                new BigDecimal("20000.00"),
                "COP",
                "Saldo WinUp",
                "https://winup.test/cartera"
        ));

        server.verify();
    }

    @Test
    void should_fetch_a_payment() {
        server.expect(requestTo("https://api.mercadopago.com/v1/payments/123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": 123,
                          "external_reference": "intent-1",
                          "transaction_amount": 50000.00,
                          "currency_id": "COP",
                          "status": "approved"
                        }
                        """, MediaType.APPLICATION_JSON));

        ProviderPayment payment = gateway.fetchPayment("123");

        assertThat(payment.externalReference()).isEqualTo("intent-1");
        assertThat(payment.amount()).isEqualByComparingTo("50000.00");
        assertThat(payment.currency()).isEqualTo("COP");
        assertThat(payment.status()).isEqualTo("approved");
        server.verify();
    }

    @Test
    void should_parse_a_webhook_notification() {
        ProviderNotification notification = gateway.parseNotification("""
                {"id":"event-1","type":"payment","data":{"id":"123"}}
                """);

        assertThat(notification.eventId()).isEqualTo("event-1");
        assertThat(notification.paymentReference()).isEqualTo("123");
    }
}
