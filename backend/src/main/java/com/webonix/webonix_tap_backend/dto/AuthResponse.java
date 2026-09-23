package com.webonix.webonix_tap_backend.dto;

public record AuthResponse(
        Long id,
        String name,
        String email,
        String role,
        String token,
        String message
) {
}