package com.winup.service.payment;

public interface PayoutGateway {
    String provider();

    PayoutResult payout(PayoutOrder order);
}
