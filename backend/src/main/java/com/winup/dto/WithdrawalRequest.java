package com.winup.dto;

public record WithdrawalRequest(String amount, String destination) {
    public WithdrawalRequest(String amount) {
        this(amount, null);
    }
}
