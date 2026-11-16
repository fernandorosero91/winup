package com.winup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "winup.sports.quota")
public record SportsQuotaProperties(int dailyLimit) {
    public SportsQuotaProperties {
        dailyLimit = dailyLimit <= 0 ? 100 : dailyLimit;
    }
}
