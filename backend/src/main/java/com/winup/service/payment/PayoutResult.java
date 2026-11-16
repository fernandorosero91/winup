package com.winup.service.payment;

public record PayoutResult(String providerReference, PayoutState state) {
}
