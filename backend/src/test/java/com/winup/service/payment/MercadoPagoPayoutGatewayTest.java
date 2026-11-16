package com.winup.service.payment;

import com.winup.config.MercadoPagoProperties;
import com.winup.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpStatus.ACCEPTED;

class MercadoPagoPayoutGatewayTest {
    private final RestTemplate restTemplate = new RestTemplate();
    private final MercadoPagoPayoutGateway gateway = new MercadoPagoPayoutGateway(
            restTemplate,
            new MercadoPagoProperties(
                    "test-token",
                    "test-public-key",
                    true,
                    "https://api.winup.test/webhooks/mercadopago",
                    "",
                    "https://winup.test/cartera"
            )
    );
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        restTemplate.setUriTemplateHandler(
                new org.springframework.web.util.DefaultUriBuilderFactory("https://api.mercadopago.com")
        );
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void should_create_a_payout_for_the_destination_account() {
        UUID withdrawalId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        server.expect(requestTo("https://api.mercadopago.com/v1/payouts"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Idempotency-Key", withdrawalId.toString()))
                .andExpect(content().string(containsString("\"email\":\"destino@testuser.com\"")))
                .andExpect(content().string(containsString("\"currency\":\"COP\"")))
                .andExpect(content().string(containsString("\"value\":50000.00")))
                .andRespond(withStatus(ACCEPTED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"id":"POP-1","status":"created"}
                                """));

        PayoutResult result = gateway.payout(new PayoutOrder(
                withdrawalId,
                new BigDecimal("50000.00"),
                "COP",
                "destino@testuser.com"
        ));

        assertThat(result.providerReference()).isEqualTo("POP-1");
        assertThat(result.state()).isEqualTo(PayoutState.PENDING);
        server.verify();
    }

    @Test
    void should_require_a_destination_account() {
        assertThatThrownBy(() -> gateway.payout(new PayoutOrder(
                UUID.randomUUID(),
                new BigDecimal("50000.00"),
                "COP",
                null
        )))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("cuenta destino");
    }

    @Test
    void should_report_the_mercado_pago_provider() {
        assertThat(gateway.provider()).isEqualTo("MERCADOPAGO");
    }

    @Test
    void should_mark_the_payout_as_failed_when_the_api_rejects_it() {
        server.expect(requestTo("https://api.mercadopago.com/v1/payouts"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        PayoutResult result = gateway.payout(new PayoutOrder(
                UUID.randomUUID(),
                new BigDecimal("50000.00"),
                "COP",
                "destino@testuser.com"
        ));

        assertThat(result.state()).isEqualTo(PayoutState.FAILED);
    }
}
