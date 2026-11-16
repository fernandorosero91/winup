package com.winup.dto;

import java.time.Instant;

public record AdminUserDto(
        String id,
        String email,
        String displayName,
        String role,
        String status,
        Instant createdAt
) {
}
