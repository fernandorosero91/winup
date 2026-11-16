package com.winup.dto;

public record ProviderHealthDto(
        String provider,
        boolean available,
        int remainingQuota,
        String message
) {
}
