package com.winup.service.payment;

public interface PaymentGateway {
    String provider();

    CheckoutSession createDeposit(DepositOrder order);

    ProviderPayment fetchPayment(String providerReference);

    ProviderNotification parseNotification(String payload);
}
