package com.webonix.webonix_tap_backend.dto;

public record LoginRequest(
        String email,
        String password
) {
}