package com.winup.dto;

public record UserDto(
        String id,
        String email,
        String displayName,
        String role
) {
}
