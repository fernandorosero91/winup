package com.winup.dto;

public record DepositRequest(String amount, String returnUrl) {
    public DepositRequest(String amount) {
        this(amount, null);
    }
}
