package com.winup.dto;

public record LoginRequest(
        String email,
        String password
) {
}
