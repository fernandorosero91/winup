package com.winup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "winup.payments.withdrawals")
public record WithdrawalOptions(BigDecimal approvalThreshold, BigDecimal dailyLimit) {
    public WithdrawalOptions {
        approvalThreshold = approvalThreshold == null ? new BigDecimal("200000.00") : approvalThreshold;
        dailyLimit = dailyLimit == null ? BigDecimal.ZERO : dailyLimit;
    }
}
