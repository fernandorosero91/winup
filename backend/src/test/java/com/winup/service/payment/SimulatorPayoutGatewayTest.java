package com.winup.service.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatorPayoutGatewayTest {
    private final SimulatorPayoutGateway gateway = new SimulatorPayoutGateway();

    @Test
    void should_report_the_simulator_provider() {
        assertThat(gateway.provider()).isEqualTo("SIMULATOR");
    }

    @Test
    void should_pay_immediately() {
        PayoutResult result = gateway.payout(new PayoutOrder(
                UUID.randomUUID(),
                new BigDecimal("50000.00"),
                "COP",
                null
        ));

        assertThat(result.state()).isEqualTo(PayoutState.PAID);
        assertThat(result.providerReference()).startsWith("SIM-");
    }
}
