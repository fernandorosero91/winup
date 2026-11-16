package com.winup.service.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "winup.payments.payout-provider", havingValue = "simulator", matchIfMissing = true)
public class SimulatorPayoutGateway implements PayoutGateway {
    @Override
    public String provider() {
        return "SIMULATOR";
    }

    @Override
    public PayoutResult payout(PayoutOrder order) {
        return new PayoutResult("SIM-" + UUID.randomUUID(), PayoutState.PAID);
    }
}
