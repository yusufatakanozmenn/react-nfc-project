package com.webonix.webonix_tap_backend.dto;

public record RegisterRequest(
        String name,
        String email,
        String password
) {
}