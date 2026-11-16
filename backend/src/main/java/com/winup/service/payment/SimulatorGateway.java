package com.winup.service.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "winup.payments.provider", havingValue = "simulator", matchIfMissing = true)
public class SimulatorGateway implements PaymentGateway {
    @Override
    public String provider() {
        return "SIMULATOR";
    }

    @Override
    public CheckoutSession createDeposit(DepositOrder order) {
        return new CheckoutSession(
                "sim-" + order.intentId(),
                "/cartera?simular=" + order.intentId()
        );
    }

    @Override
    public ProviderPayment fetchPayment(String providerReference) {
        throw new UnsupportedOperationException("El simulador no consulta pagos");
    }

    @Override
    public ProviderNotification parseNotification(String payload) {
        throw new UnsupportedOperationException("El simulador no recibe webhooks");
    }
}
