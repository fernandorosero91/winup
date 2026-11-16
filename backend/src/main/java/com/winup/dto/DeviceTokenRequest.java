package com.winup.dto;

public record DeviceTokenRequest(
        String token,
        String platform
) {
}
