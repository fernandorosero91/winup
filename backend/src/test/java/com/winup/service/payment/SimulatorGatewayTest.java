package com.winup.service.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatorGatewayTest {
    private final SimulatorGateway gateway = new SimulatorGateway();

    @Test
    void should_return_a_local_checkout_for_the_intent() {
        UUID intentId = UUID.randomUUID();

        CheckoutSession session = gateway.createDeposit(
                new DepositOrder(intentId, new BigDecimal("50000.00"), "COP", "Saldo WinUp")
        );

        assertThat(gateway.provider()).isEqualTo("SIMULATOR");
        assertThat(session.providerReference()).isEqualTo("sim-" + intentId);
        assertThat(session.checkoutUrl()).contains(intentId.toString());
    }
}
