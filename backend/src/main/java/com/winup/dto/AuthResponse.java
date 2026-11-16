package com.winup.dto;

public record AuthResponse(
        String token,
        UserDto user
) {
}
